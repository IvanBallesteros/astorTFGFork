package fr.inria.astor.core.ingredientbased;

import java.util.HashMap;
import java.util.Map;

/**
 * Class to manage prompt templates for LLM-based repair.
 * This allows referencing templates by name and provides predefined templates.
 */
public class LLMPromptTemplate {
    
    // Static map of predefined templates
    private static final Map<String, String> predefinedTemplates = new HashMap<>();
    
    // Initialize predefined templates
    static {       
        
        // New template for multiple solutions
        predefinedTemplates.put(
            "MULTIPLE_SOLUTIONS",
            "There's a bug in the following Java code:\n\n{buggycode}\n" +
            "The failing test case is:\n\n{testcode}\n" +
            "Please provide {nsolutions} different possible fixes that might make the test pass.\n" +
            "Format your response as follows:\n" +
            "SOLUTION 1:\n[first solution code]\n" +
            "SOLUTION 2:\n[second solution code]\n" +
            ".... SOLUTION {nsolutions}:\n[third solution code]\n" +
            "Each solution should be a single line of code that can replace the buggy line, do not write code in different lines." +
            "Do not include extra explanations, just the code solutions."
        );
        predefinedTemplates.put(
            "UNIQUE_SOLUTION",
            "There's a bug in the following Java code:\n\n{buggycode}\n" +
            "The failing test case is:\n\n{testcode}\n" +
            "Please provide one possible fix that might make the test pass.\n" +
            "Format your response as follows:\n" +
            "SOLUTION:\n[first solution code]\n" +
            "Solution should be a single line of code that can replace the buggy line, do not write code in different lines." +
            "Do not include extra explanations, just the code solutions."
        );
        predefinedTemplates.put(
            "EASY_SOLUTIONS",
            "There's a bug in the following Java code:\n\n{buggycode}\n" +
            "The failing test case is:\n\n{testcode}\n" +
            "Please provide {nsolutions} possible fixes that might make the test pass.\n" +
            "Solution should be a single line of code that can replace the buggy line, do not write code in different lines." +
            "Do not include extra explanations, just the code solutions." +
            "Format your response as follows:\n" +
            "SOLUTION 1:\n[solution code]\n" +
            "SOLUTION 2:\n[solution code]\n" +
            "SOLUTION 3:\n[solution code]\n" +
            "Don't justify any response, just provide the code solutions."
        );
        predefinedTemplates.put(
            "STRICT_SOLUTIONS",
            "You are an automated code-fixing agent. Your job is to replace a buggy Java line with working alternatives.\n\n" +
            "Buggy Java code:\n{buggycode}\n\n" +
            "Failing test case:\n{testcode}\n\n" +
            "Generate {nsolutions} single-line Java code replacements that may fix the bug.\n" +
            "⚠️ DO NOT include any explanations or extra text.\n" +
            "⚠️ ONLY output code, strictly formatted as:\n" +
            "SOLUTION 1:\n<code>\n" +
            "SOLUTION 2:\n<code>\n" +
            "SOLUTION 3:\n<code>\n" +
            "Do not include comments, justification, or multiple lines. Only single-line replacement code."
        );
        predefinedTemplates.put(
            "HELPED_SOLUTIONS",
            "You are an automated code-fixing agent. Your job is to replace a buggy Java line with working alternatives.\n\n" +
            "Buggy Java code:\n{buggycode}\n\n" +
            "Failing test case:\n{testcode}\n\n" +
            "Generate {nsolutions} single-line Java code replacements that may fix the bug.\n" +
            "Please, apply just one edit,  replace < by >= \n" +
            "⚠️ DO NOT include any explanations or extra text.\n" +
            "⚠️ ONLY output code, strictly formatted as:\n" +
            "SOLUTION 1:\n<code>\n" +
            "SOLUTION 2:\n<code>\n" +
            ".... SOLUTION {nsolutions}:\n[third solution code]\n" +
            "Do not include comments, justification, or multiple lines. Only single-line replacement code."
        );
        predefinedTemplates.put(
            "GUIDED_SOLUTIONS",
            "You are an automated code-fixing agent. Your job is to replace a buggy Java line with working alternatives.\n" +
            "Buggy Java code:\n{buggycode}\n\n" +
            "Failing test case:\n{testcode}\n\n" +
            "Generate {nsolutions} single-line Java code replacements that may fix the bug.\n" +
            "Please, apply just one small edit, change operator '<' for '>= " +
            "⚠️ DO NOT include any explanations or extra text.\n" +
            "⚠️ ONLY output code, strictly formatted as:\n" +
            "SOLUTION 1:\n<code>\n" +
            "SOLUTION 2:\n<code>\n" +
            ".... SOLUTION {nsolutions}:\n[third solution code]\n" 
        );

    }
    
    /**
     * Get a template by name
     * 
     * @param templateName The name of the predefined template
     * @return The template string or null if not found
     */
    public static String getTemplate(String templateName) {
        return predefinedTemplates.get(templateName);
    }
    
    /**
     * Check if a template with the given name exists
     * 
     * @param templateName The name to check
     * @return true if the template exists, false otherwise
     */
    public static boolean hasTemplate(String templateName) {
        return predefinedTemplates.containsKey(templateName);
    }
    
    /**
     * Fill a template with the provided values
     * 
     * @param template The template string with placeholders
     * @param buggyCode The buggy code to insert
     * @param testCode The test code to insert
     * @param maxP The maximum number of solutions to provide (default is 3)
     * @return The filled template
     */
    public static String fillTemplate(String template, String buggyCode, String testCode, int maxP) {
        return template
                .replace("{buggycode}", buggyCode)
                .replace("{testcode}", testCode)
                .replace("{nsolutions}", String.valueOf(maxP));
    }
    
    /**
     * Get a filled template by name
     * 
     * @param templateName The name of the template
     * @param buggyCode The buggy code to insert
     * @param testCode The test code to insert
     * @return The filled template or null if template not found
     */
    public static String getFilledTemplate(String templateName, String buggyCode, String testCode, int maxP) {
        String template = getTemplate(templateName);
        if (template == null) {
            return null;
        }
        return fillTemplate(template, buggyCode, testCode, maxP);
    }
}