package de.hpi.isg.mdms.tools.sql;

import antlrd.SQLiteBaseListener;
import antlrd.SQLiteLexer;
import antlrd.SQLiteParser;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class can parse SQL files and specifically extract the contents of {@code CREATE TABLE} statements.
 */
public class SQLParser {

    /**
     * Parse the given SQL file and create a {@link Map} of its tables and columns.
     *
     * @param sqlFile the path to a SQL file
     * @return a {@link Map} that associates table names to the list of their column names;
     * the table names are normalized (lower-cased)
     */
    public static Map<String, List<String>> parseSchema(String sqlFile) {

        Map<String, List<String>> columnNamesMap = new HashMap<>();

        List<String> sqlStatements = loadStatements(sqlFile);

        for (int i = 0; i < sqlStatements.size(); i++) {

            List<String> tableName = new ArrayList<>();
            List<String> columnNames = new ArrayList<>();

            SQLiteLexer lexer = new SQLiteLexer(new ANTLRInputStream(sqlStatements.get(i)));
            SQLiteParser parser = new SQLiteParser(new CommonTokenStream(lexer));

            //avoid warnings in console
            parser.removeErrorListeners();

            ParseTree tree = parser.sql_stmt();

            // Walk the `create_table_stmt` production and listen when the parser enters the column_defs.
            ParseTreeWalker.DEFAULT.walk(new SQLiteBaseListener() {
                @Override
                public void enterCreate_table_stmt(@NotNull SQLiteParser.Create_table_stmtContext ctx) {
                    if (ctx.table_name() != null) {
                        tableName.add(ctx.table_name().getText());
                    }
                }

                @Override
                public void enterColumn_def(@NotNull SQLiteParser.Column_defContext ctx) {
                    if (ctx.column_name() != null) {
                        columnNames.add(ctx.column_name().getText());
                    }
                }
            }, tree);
            if (!tableName.isEmpty()) {
                columnNamesMap.put(tableName.get(0).toLowerCase(), columnNames);
            }
        }
        return columnNamesMap;
    }

    /**
     * Parse the given SQL file and collect any primary key definitions. Specifically this method handles
     * <ul>
     * <li>{@code CREATE TABLE} statements, and</li>
     * <li>{@code ALTER TABLE} statements.</li>
     * </ul>
     *
     * @param sqlFile the path to a SQL file
     * @return {@link PrimaryKeyDefinition}s
     */
    public static Collection<PrimaryKeyDefinition> parsePrimaryKeys(String sqlFile) {

        Collection<PrimaryKeyDefinition> primaryKeys = new LinkedList<>();
        List<String> sqlStatements = loadStatements(sqlFile);
        for (String sqlStatement : sqlStatements) {

            SQLiteLexer lexer = new SQLiteLexer(new ANTLRInputStream(sqlStatement));
            SQLiteParser parser = new SQLiteParser(new CommonTokenStream(lexer));
            //avoid warnings in console
            parser.removeErrorListeners();
            ParseTree tree = parser.sql_stmt();

            // Walk the `create_table_stmt` production and listen when the parser enters the column_defs.
            ParseTreeWalker.DEFAULT.walk(new SQLiteBaseListener() {

                /** Keeps track of the table name in the current parsing context. */
                String tableName;

                /** Keeps track of the column name in the current parsing context. */
                String columnName;

                @Override
                public void enterCreate_table_stmt(SQLiteParser.Create_table_stmtContext ctx) {
                    super.enterCreate_table_stmt(ctx);
                    if (ctx.table_name() != null) {
                        this.tableName = ctx.table_name().getText();
                    }
                }

                @Override
                public void enterAlter_table_stmt(SQLiteParser.Alter_table_stmtContext ctx) {
                    super.enterAlter_table_stmt(ctx);
                    if (ctx.table_name() != null) {
                        this.tableName = ctx.table_name().getText();
                    }
                }

                @Override
                public void enterColumn_def(SQLiteParser.Column_defContext ctx) {
                    super.enterColumn_def(ctx);
                    this.columnName = ctx.column_name().getText();
                }

                @Override
                public void enterColumn_constraint(SQLiteParser.Column_constraintContext ctx) {
                    super.enterColumn_constraint(ctx);

                    // Check whether this is actually a PK definition.
                    if (ctx.K_PRIMARY() == null) return;

                    // Make sure that we have a table name.
                    if (this.tableName == null || this.columnName == null) return;

                    primaryKeys.add(new PrimaryKeyDefinition(this.tableName, this.columnName));
                }

                @Override
                public void enterTable_constraint(SQLiteParser.Table_constraintContext ctx) {
                    super.enterTable_constraint(ctx);

                    // Check whether this is actually a PK definition.
                    if (ctx.K_PRIMARY() == null) return;

                    // Make sure that we have a table name.
                    if (this.tableName == null) return;

                    List<String> columns = new ArrayList<>();
                    for (SQLiteParser.Indexed_columnContext columnCtx : ctx.indexed_column()) {
                        columns.add(columnCtx.column_name().getText());
                    }
                    primaryKeys.add(new PrimaryKeyDefinition(this.tableName, columns));
                }

            }, tree);
        }
        return primaryKeys;
    }

    /**
     * Parse the given SQL file and collect any foreign key definitions. Specifically this method handles
     * <ul>
     * <li>{@code CREATE TABLE} statements, and</li>
     * <li>{@code ALTER TABLE} statements.</li>
     * </ul>
     *
     * @param sqlFile the path to a SQL file
     * @return {@link ForeignKeyDefinition}s
     */
    public static Collection<ForeignKeyDefinition> parseForeignKeys(String sqlFile) {

        Collection<ForeignKeyDefinition> foreignKeys = new LinkedList<>();
        List<String> sqlStatements = loadStatements(sqlFile);
        for (String sqlStatement : sqlStatements) {

            SQLiteLexer lexer = new SQLiteLexer(new ANTLRInputStream(sqlStatement));
            SQLiteParser parser = new SQLiteParser(new CommonTokenStream(lexer));
            //avoid warnings in console
            parser.removeErrorListeners();
            ParseTree tree = parser.sql_stmt();

            // Walk the `create_table_stmt` production and listen when the parser enters the column_defs.
            ParseTreeWalker.DEFAULT.walk(new SQLiteBaseListener() {

                /** Keeps track of the table name in the current parsing context. */
                String tableName;

                /** Keeps track of the column name in the current parsing context. */
                String columnName;

                @Override
                public void enterCreate_table_stmt(SQLiteParser.Create_table_stmtContext ctx) {
                    super.enterCreate_table_stmt(ctx);
                    if (ctx.table_name() != null) {
                        this.tableName = ctx.table_name().getText();
                    }
                }

                @Override
                public void enterAlter_table_stmt(SQLiteParser.Alter_table_stmtContext ctx) {
                    super.enterAlter_table_stmt(ctx);
                    if (ctx.table_name() != null) {
                        this.tableName = ctx.table_name().getText();
                    }
                }

                @Override
                public void enterColumn_def(SQLiteParser.Column_defContext ctx) {
                    super.enterColumn_def(ctx);
                    this.columnName = ctx.column_name().getText();
                }

                @Override
                public void enterColumn_constraint(SQLiteParser.Column_constraintContext ctx) {
                    super.enterColumn_constraint(ctx);

                    // Check whether this is actually a FK definition.
                    if (ctx.foreign_key_clause() == null) return;

                    // Make sure that we have a table name.
                    if (this.tableName == null || this.columnName == null) return;

                    foreignKeys.add(this.createForeignKeyDefinition(ctx.foreign_key_clause(), this.tableName, this.columnName));
                }

                @Override
                public void enterTable_constraint(SQLiteParser.Table_constraintContext ctx) {
                    super.enterTable_constraint(ctx);

                    // Check whether this is actually a PK definition.
                    if (ctx.K_FOREIGN() == null) return;

                    // Make sure that we have a table name.
                    if (this.tableName == null) return;

                    foreignKeys.add(this.createForeignKeyDefinition(ctx));
                }

                /**
                 * Helper method to create a {@link ForeignKeyDefinition}.
                 *
                 * @param tableConstraint that describes a foreign key
                 * @return the {@link ForeignKeyDefinition}
                 */
                private ForeignKeyDefinition createForeignKeyDefinition(SQLiteParser.Table_constraintContext tableConstraint) {
                    List<String> referencedColumns = tableConstraint.column_name().stream().map(RuleContext::getText).collect(Collectors.toList());
                    return this.createForeignKeyDefinition(
                            tableConstraint.foreign_key_clause(),
                            this.tableName,
                            referencedColumns.toArray(new String[referencedColumns.size()])
                    );
                }

                /**
                 * Helper method to create a {@link ForeignKeyDefinition}.
                 *
                 * @param referencesClause that describes the `REFERENCES` part of the foreign key
                 * @param dependentTable the dependent table
                 * @param dependentColumns the referenced tables
                 * @return the {@link ForeignKeyDefinition}
                 */
                private ForeignKeyDefinition createForeignKeyDefinition(
                        SQLiteParser.Foreign_key_clauseContext referencesClause,
                        String dependentTable,
                        String... dependentColumns
                ) {
                    return new ForeignKeyDefinition(
                            dependentTable, Arrays.asList(dependentColumns),
                            referencesClause.foreign_table().getText(),
                            referencesClause.column_name().stream().map(RuleContext::getText).collect(Collectors.toList())
                    );
                }

            }, tree);
        }
        return foreignKeys;
    }

    private static List<String> loadStatements(String fileLocation) {
        List<String> sqlStatements = new ArrayList<>();
        try {
            Scanner infile = new Scanner(new File(fileLocation)).useDelimiter(";");
            while (infile.hasNext()) {
                sqlStatements.add(infile.next().replace("()", ""));
            }
            infile.close();
            return sqlStatements;
        } catch (Exception ex) {
            System.out.println(ex);
        }
        return sqlStatements;
    }

}