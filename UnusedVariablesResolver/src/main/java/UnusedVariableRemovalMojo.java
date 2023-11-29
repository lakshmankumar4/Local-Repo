import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Mojo(name = "remove-unused-variables")
public class UnusedVariableRemovalMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.basedir}/src/main/java")
    private String sourceDirectory;

    public void execute() throws MojoExecutionException {
        try {
            Files.walk(Paths.get(sourceDirectory))
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(this::removeUnusedVariables);
        } catch (IOException e) {
            throw new MojoExecutionException("Error while processing files", e);
        }
    }

    private void removeUnusedVariables(Path filePath) {
        try {
            String code = new String(Files.readAllBytes(filePath));
            ParseResult<CompilationUnit> parseResult = new JavaParser().parse(code);
            CompilationUnit cu = parseResult.getResult().orElse(null);

            if (cu != null) {
                UnusedVariableVisitor variableVisitor = new UnusedVariableVisitor();

                List<String> variableNames = new ArrayList<>();
                variableVisitor.visit(cu, variableNames);
                for (String variable : variableNames) {
                    System.out.println("Unused variable: " + variable);
                }
                Files.write(filePath, LexicalPreservingPrinter.print(cu).getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class UnusedVariableVisitor extends VoidVisitorAdapter<List<String>> {
       List<String> allVariables = new ArrayList<>();

        @Override
        public void visit(VariableDeclarator declarator, List<String> collector) {
            super.visit(declarator, collector);
            allVariables.add(declarator.getName().asString());
            if (declarator.getInitializer().isPresent()) {
                Expression expression = declarator.getInitializer().get();
                if (expression.isBinaryExpr()) {
                    String left = ((BinaryExpr) expression).getLeft().toString();
                    String right = ((BinaryExpr) expression).getRight().toString();
                    if (allVariables.contains(left)) {
                        if (collector.contains(left)) {
                            collector.remove(left);
                        } else {
                            collector.add(declarator.getName().asString());
                        }
                    }
                    if (allVariables.contains(right)) {
                        if (collector.contains(right)) {
                            collector.remove(right);
                        } else {
                            collector.add(declarator.getName().asString());
                        }
                    }
                }
                if (expression.isIntegerLiteralExpr()) {
                    String integerExpression = expression.asIntegerLiteralExpr().toString();
                    if (!allVariables.contains(integerExpression)) {
                        if (collector.contains(integerExpression)) {
                            collector.remove(declarator.getName().asString());
                        } else {
                            collector.add(declarator.getName().asString());
                        }
                    }
                }
            }
        }
    }
}
