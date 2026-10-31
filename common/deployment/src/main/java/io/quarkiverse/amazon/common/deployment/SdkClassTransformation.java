package io.quarkiverse.amazon.common.deployment;

import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASM9;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

import org.jboss.logging.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;

/**
 * Transforms an AWS SDK class used for Vert.x context propagation.
 * <p>
 * The class is checked before being transformed. If an AWS SDK version, e.g. one pinned by the application, changed the
 * transformed code, the transformation is skipped with a warning rather than failing the build: the SDK then keeps its own
 * completion behavior.
 */
public final class SdkClassTransformation {
    private static final Logger LOG = Logger.getLogger(SdkClassTransformation.class);

    private SdkClassTransformation() {
    }

    /** A visitor reporting whether the visited class has the expected shape. Its delegate is {@code null} during the check. */
    public abstract static class CheckedVisitor extends ClassVisitor {
        protected CheckedVisitor(ClassVisitor visitor) {
            super(ASM9, visitor);
        }

        /** @return why the class cannot be transformed, or {@code null} once the whole class is visited and supported */
        protected abstract String mismatch();
    }

    /** @return the transformation, or {@code null} if the class is missing or unsupported */
    public static BytecodeTransformerBuildItem transform(String className, Function<ClassVisitor, CheckedVisitor> visitor) {
        byte[] bytes;
        try (InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(className.replace('.', '/') + ".class")) {
            if (stream == null) {
                return null;
            }
            bytes = stream.readAllBytes();
        } catch (IOException e) {
            LOG.warnf(e, "Unable to read %s; Vert.x context propagation is disabled for it", className);
            return null;
        }
        String mismatch = check(bytes, visitor);
        if (mismatch != null) {
            LOG.warnf("Vert.x context propagation is disabled for %s, which is not supported by this AWS SDK version: %s",
                    className, mismatch);
            return null;
        }
        return new BytecodeTransformerBuildItem(className, (name, delegate) -> visitor.apply(delegate));
    }

    static String check(byte[] bytes, Function<ClassVisitor, CheckedVisitor> visitor) {
        CheckedVisitor check = visitor.apply(null);
        new ClassReader(bytes).accept(check, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return check.mismatch();
    }

    /**
     * Wraps the result of the methods returning the future of an operation. Exactly the expected number of methods must
     * match.
     */
    public abstract static class FinalResultVisitor extends CheckedVisitor {
        private static final MethodVisitor NO_OP = new MethodVisitor(ASM9) {
        };
        private final int expectedMethods;
        private final String description;
        private int methods;
        private int returns;

        protected FinalResultVisitor(ClassVisitor visitor, int expectedMethods, String description) {
            super(visitor);
            this.expectedMethods = expectedMethods;
            this.description = description;
        }

        protected abstract boolean matches(String name, String descriptor);

        /** Called on method entry. */
        protected void enter(MethodVisitor visitor) {
        }

        /** Called with the returned future on the stack, which must be replaced by the wrapped future. */
        protected abstract void wrap(MethodVisitor visitor);

        /** Called once all the instructions of the method are visited. */
        protected void exit(MethodVisitor visitor) {
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (!matches(name, descriptor)) {
                return visitor;
            }
            methods++;
            return new MethodVisitor(ASM9, visitor) {
                MethodVisitor target() {
                    // The delegate is null while checking the class
                    return mv == null ? NO_OP : mv;
                }

                @Override
                public void visitCode() {
                    super.visitCode();
                    enter(target());
                }

                @Override
                public void visitInsn(int opcode) {
                    if (opcode == ARETURN) {
                        wrap(target());
                        returns++;
                    }
                    super.visitInsn(opcode);
                }

                @Override
                public void visitMaxs(int maxStack, int maxLocals) {
                    exit(target());
                    super.visitMaxs(maxStack, maxLocals);
                }
            };
        }

        @Override
        protected String mismatch() {
            if (methods != expectedMethods || returns < methods) {
                return "expected " + expectedMethods + " " + description + ", found " + methods + " methods and " + returns
                        + " returns";
            }
            return null;
        }
    }
}
