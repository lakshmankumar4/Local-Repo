import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Testing {
    public static void main(String[] args) {
        removeUnusedVariables(Path.of("C:\\Users\\HP\\Desktop\\SampleProgram.java"));
    }

    private static void  removeUnusedVariables(Path filePath) {
        try {
            System.out.println("Entered 2");
            String code = new String(Files.readAllBytes(filePath));
            ParseResult<CompilationUnit> parseResult = new JavaParser().parse(code);
            CompilationUnit cu = parseResult.getResult().orElse(null);

            if (cu != null) {
                UnusedVariableVisitor variableVisitor = new UnusedVariableVisitor();
                // Create a list to hold variable names
                List<String> variableNames = new ArrayList<>();
                variableVisitor.visit(cu, variableNames);
                for (String variable : variableNames) {
                    if (!cu.toString().contains(variable)) {
                        System.out.println("Unused variable: " + variable);
                    }
                }
                Files.write(filePath, LexicalPreservingPrinter.print(cu).getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Handle IO exceptions
        }
    }

    private static class UnusedVariableVisitor extends VoidVisitorAdapter<List<String>> {

        @Override
        public void visit(VariableDeclarator declarator, List<String> collector) {
            super.visit(declarator, collector);
            collector.add(declarator.getName().asString());
        }

        private boolean variableIsUsed(VariableDeclarator variable) {
            // Add logic to determine if the variable is used in the code
            // This might involve analyzing references, assignments, etc.
            // For simplicity, assuming the variable is unused for demonstration
            return false;
        }
    }
}
