/*
`DIRECTORY STRUCTURE:
src/main/java/com/example/datagenerator/expression/functions/
├── ExpressionFunction.java                 (Interface)
├── FunctionExecutionContext.java          (Context class)
├── RandomNumberFunction.java              (randomNumber function)
├── SubstringFunction.java                 (substring function)
├── CaseTransformFunctions.java            (uppercase, lowercase, replace, concat, length)
└── PaddingAndAdvancedFunctions.java       (padLeft, padRight, formatDate, randomChoice, conditional)`

**USAGE EXAMPLES:**

1. Simple field concatenation:
 `  Expression: "${firstName}_${lastName}_${randomNumber(1000,9999)}"`
   Result: "John_Smith_1234"

2. Conditional formatting:
  ` Expression: "conditional(${rowIndex} > 10, 'PREMIUM', 'STANDARD')"`
   Result: "PREMIUM" or "STANDARD" based on row index

3. Complex transformations:
  ` Expression: "padLeft(uppercase(substring(${lastName}, 0, 3)), 5, '0')"`
   Result: "00SMI" (for lastName "Smith")

4. Date formatting:
   `Expression: "formatDate('yyyy-MM-dd')"`
   Result: "2024-01-15"

5. Random choices:
   `Expression: "randomChoice('red', 'blue', 'green', 'yellow')"`
   Result: One of the color options randomly

6. String manipulation:
   `Expression: "replace(${email}, '@', '_AT_')"`
   Result: "user_AT_domain.com" (for email "user@domain.com")

**ERROR HANDLING:**
- All functions include proper parameter validation
- Invalid parameters throw IllegalArgumentException with descriptive messages
- Field references that don't exist return empty strings
- Function errors are caught and marked with [FUNCTION_ERROR:...] in output

**EXTENDING THE SYSTEM:**
To add new functions:
1. Implement the ExpressionFunction interface
2. Register in initializeFunctions() method
3. Function will be automatically available in expressions
   */