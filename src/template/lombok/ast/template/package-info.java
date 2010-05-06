/**
 * This package contains an Annotation Processor that will turn AST Node templates into the actual AST Node source files.
 * This package is only required to process the source files; it should not be included in the runtime distribution of {@code lombok.ast}.
 * <p />
 * To create an AST Node template for a hypothetical node named {@code If}, create a class annotated with {@link GenerateAstNode} whose name
 * ends in {@code Template}. Each field then represents an AST field and will automatically be treated as such. Methods are normally ignored
 * unless you annotate them with one of the annotations in this package. Specifically:
 * <dl>
 * <dt>{@link lombok.ast.template.CopyMethod}</dt>
 * <dd>A wrapper method will be generated in the AST Node class that calls this method. It has the same name. Annotated method must be static,
 * and the first parameter must be the generated type, e.g. {@code @CopyMethod public static boolean foo(If self, int someOtherParam) ...}.</dd>
 * <dt>{@link lombok.ast.template.InitialValue}</dt>
 * <dd>Use to specify a default initial value for the field. By default there is no initial value (JVM default of null).</dd>
 * <dt>{@link lombok.ast.template.NotChildOfNode}</dt>
 * <dd>Use to specify a field does not contain an AST Node but a terminal, such as for example the name of an identifier node.</dd>
 * <dt>{@link lombok.ast.template.SyntaxCheck}</dt>
 * <dd>Annotates a method to indicate it must be called to check if the node is syntactically valid. Can also be put on a type to indicate all methods in it are syntax checks.</dd>
 * <dt>{@link lombok.ast.template.Mandatory}</dt>
 * <dd>Indicates the field would not be {@code null} in a syntactically valid AST tree.</dd>
 * </dl>
 * See the javadoc of each annotation for more detailed information about how to use them.
 * 
 * Examples can be found in {@code src/main/lombok/ast/Templates.java}.
 */
package lombok.ast.template;
