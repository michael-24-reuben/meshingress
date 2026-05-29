package dev.mrk.meshingress.artifact.scope;

import sootup.core.model.SootMethod;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ArrayType;
import sootup.core.types.ClassType;
import sootup.core.types.PrimitiveType;
import sootup.core.types.Type;
import sootup.core.types.VoidType;

record MethodReference(
        String owner,
        String name,
        String descriptor
) {
    static MethodReference fromSootMethod(SootMethod method) {
        return fromMethodSignature(method.getSignature());
    }

    static MethodReference fromMethodSignature(MethodSignature signature) {
        StringBuilder descriptor = new StringBuilder("(");
        for (Type parameterType : signature.getParameterTypes()) {
            descriptor.append(typeDescriptor(parameterType));
        }
        descriptor.append(')').append(typeDescriptor(signature.getType()));
        return new MethodReference(
                signature.getDeclClassType().getFullyQualifiedName().replace('.', '/'),
                signature.getName(),
                descriptor.toString()
        );
    }

    String display() {
        return owner + "." + name + descriptor;
    }

    String ownerClassName() {
        return owner.replace('/', '.');
    }

    private static String typeDescriptor(Type type) {
        if (type instanceof VoidType) {
            return "V";
        }
        if (type instanceof PrimitiveType primitiveType) {
            return switch (primitiveType.getName()) {
                case "boolean" -> "Z";
                case "byte" -> "B";
                case "char" -> "C";
                case "short" -> "S";
                case "int" -> "I";
                case "long" -> "J";
                case "float" -> "F";
                case "double" -> "D";
                default -> throw new IllegalArgumentException("Unsupported primitive type: " + primitiveType);
            };
        }
        if (type instanceof ArrayType arrayType) {
            return "[".repeat(arrayType.getDimension()) + typeDescriptor(arrayType.getBaseType());
        }
        if (type instanceof ClassType classType) {
            return "L" + classType.getFullyQualifiedName().replace('.', '/') + ";";
        }
        throw new IllegalArgumentException("Unsupported method signature type: " + type);
    }
}
