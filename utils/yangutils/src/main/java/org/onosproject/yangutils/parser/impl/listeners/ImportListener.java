/*
 * Copyright 2016 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.yangutils.parser.impl.listeners;

import org.onosproject.yangutils.datamodel.YangImport;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangSubModule;
import org.onosproject.yangutils.parser.Parsable;
import org.onosproject.yangutils.parser.ParsableDataType;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation;

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ABNF grammar as per RFC6020
 * linkage-stmts       = ;; these stmts can appear in any order
 *                       *(import-stmt stmtsep)
 *                       *(include-stmt stmtsep)
 *
 * import-stmt         = import-keyword sep identifier-arg-str optsep
 *                       "{" stmtsep
 *                           prefix-stmt stmtsep
 *                           [revision-date-stmt stmtsep]
 *                        "}"
 *
 * ANTLR grammar rule
 * linkage_stmts : (import_stmt
 *               | include_stmt)*;
 * import_stmt : IMPORT_KEYWORD IDENTIFIER LEFT_CURLY_BRACE import_stmt_body
 *               RIGHT_CURLY_BRACE;
 * import_stmt_body : prefix_stmt revision_date_stmt?;
 */

/**
 * Implements listener based call back function corresponding to the "import"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class ImportListener {

    /**
     * Creates a new import listener.
     */
    private ImportListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar
     * rule (import), perform validations and update the data model
     * tree.
     *
     * @param listener Listener's object.
     * @param ctx context object of the grammar rule.
     */
    public static void processImportEntry(TreeWalkListener listener, GeneratedYangParser.ImportStatementContext ctx) {

        // Check for stack to be non empty.
        ListenerValidation.checkStackIsNotEmpty(listener, ListenerErrorType.MISSING_HOLDER,
                                                ParsableDataType.IMPORT_DATA,
                                                String.valueOf(ctx.IDENTIFIER().getText()),
                                                ListenerErrorLocation.ENTRY);

        YangImport importNode = new YangImport();
        importNode.setModuleName(String.valueOf(ctx.IDENTIFIER().getText()));

        // Push import node to the stack.
        listener.getParsedDataStack().push(importNode);
    }

    /**
     * It is called when parser exits from grammar rule (import), it perform
     * validations and update the data model tree.
     *
     * @param listener Listener's object.
     * @param ctx context object of the grammar rule.
     */
    public static void processImportExit(TreeWalkListener listener, GeneratedYangParser.ImportStatementContext ctx) {

        // Check for stack to be non empty.
        ListenerValidation.checkStackIsNotEmpty(listener, ListenerErrorType.MISSING_HOLDER,
                                                ParsableDataType.IMPORT_DATA,
                                                String.valueOf(ctx.IDENTIFIER().getText()),
                                                ListenerErrorLocation.EXIT);

        Parsable tmpImportNode = listener.getParsedDataStack().peek();
        if (tmpImportNode instanceof YangImport) {
            listener.getParsedDataStack().pop();

            // Check for stack to be non empty.
            ListenerValidation.checkStackIsNotEmpty(listener, ListenerErrorType.MISSING_HOLDER,
                                                    ParsableDataType.IMPORT_DATA,
                                                    String.valueOf(ctx.IDENTIFIER().getText()),
                                                    ListenerErrorLocation.EXIT);

            Parsable tmpNode = listener.getParsedDataStack().peek();
            switch (tmpNode.getParsableDataType()) {
            case MODULE_DATA: {
                YangModule module = (YangModule) tmpNode;
                module.addImportedInfo((YangImport) tmpImportNode);
                break;
            }
            case SUB_MODULE_DATA: {
                YangSubModule subModule = (YangSubModule) tmpNode;
                subModule.addImportedInfo((YangImport) tmpImportNode);
                break;
            }
            default:
                throw new ParserException(
                                          ListenerErrorMessageConstruction
                                                  .constructListenerErrorMessage(ListenerErrorType.INVALID_HOLDER,
                                                                                 ParsableDataType.IMPORT_DATA,
                                                                                 String.valueOf(ctx.IDENTIFIER()
                                                                                         .getText()),
                                                                                 ListenerErrorLocation.EXIT));
            }
        } else {
            throw new ParserException(
                                      ListenerErrorMessageConstruction
                                              .constructListenerErrorMessage(ListenerErrorType.MISSING_CURRENT_HOLDER,
                                                                             ParsableDataType.IMPORT_DATA, String
                                                                                     .valueOf(ctx.IDENTIFIER()
                                                                                             .getText()),
                                                                             ListenerErrorLocation.EXIT));
        }
    }
}