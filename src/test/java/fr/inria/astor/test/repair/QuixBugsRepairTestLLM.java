package fr.inria.astor.test.repair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Ignore;  
import org.junit.Test;

import fr.inria.astor.approaches.cardumen.CardumenApproach;
import fr.inria.astor.approaches.cardumen.CardumenExhaustiveApproach;
import fr.inria.astor.approaches.cardumen.ExpressionReplaceOperator;
import fr.inria.astor.approaches.cardumholes.Cardumen1HApproach;
import fr.inria.astor.approaches.jgenprog.extension.TibraApproach;
import fr.inria.astor.core.entities.Ingredient;
import fr.inria.astor.core.entities.ModificationPoint;
import fr.inria.astor.core.entities.OperatorInstance;
import fr.inria.astor.core.entities.SuspiciousModificationPoint;
import fr.inria.astor.core.faultlocalization.flacoco.FlacocoFaultLocalization;
import fr.inria.astor.core.ingredientbased.IngredientBasedApproach;
import fr.inria.astor.core.ingredientbased.LLMIngredientEngine;
import fr.inria.astor.core.setup.ConfigurationProperties;
import fr.inria.astor.core.solutionsearch.AstorCoreEngine;
import fr.inria.astor.core.solutionsearch.spaces.ingredients.IngredientPoolLocationType;
import fr.inria.astor.core.solutionsearch.spaces.ingredients.transformations.NoIngredientTransformation;
import fr.inria.astor.core.stats.PatchHunkStats;
import fr.inria.astor.core.stats.PatchStat;
import fr.inria.astor.core.stats.PatchStat.HunkStatEnum;
import fr.inria.astor.core.stats.PatchStat.PatchStatEnum;
import fr.inria.main.AstorOutputStatus;
import fr.inria.main.CommandSummary;
import fr.inria.main.ExecutionMode;
import fr.inria.main.evolution.AstorMain;
import fr.inria.main.evolution.ExtensionPoints;

/**
 * Testing all repaired bugs by Astor from Quixbugs from our experiment
 * 
 * @author Matias Martinez
 *
 */
public class QuixBugsRepairTestLLM {
    
    // Lists to track test results
    private static List<String> passedTests = new ArrayList<>();
    private static List<String> failedTests = new ArrayList<>();
    
    // Map to track execution time for each test
    private static Map<String, Long> testExecutionTimes = new HashMap<>();
    
    // LLM configuration parameters
    private static String llmService;
    private static String llmModel;
    private static int maxSuggestionsPerPoint;
    private static String llmPromptTemplate;
    
    // List of all QuixBugs tests to run
    private static List<String> quixBugsTests = Arrays.asList(
        "bitcount",
		"breadth_first_search",
		"bucketsort",
		"depth_first_search",
		"detect_cycle",
		"find_first_in_sorted",
		"find_in_sorted",
		"flatten",
		"gcd",
		"get_factors",
		"hanoi",
		"is_valid_parentheses",
		"khepsack",
		"kth",
		"lcs_length",
		"levenshtein",
		"lis",
		"longest_common_subsequence",
		"max_sublist_sum",
		"mergesort",
		"minimum_spanning_tree",
		"next_palindrome",
		"next_permutation",
		"pascal",
		"possible_change",
		"powerset",
		"quicksort",
		"reverse_linked_list",
		"rpn_eval",
		"shortest_path_length",
		"shortest_path_lenghts",
		"shortest_paths",
		"shunting_yard",
		"sieve",
		"sqrt",
		"subsequences",
		"to_base",
		"topological_ordering",
		"wrap"
    );
    
    // List of tests to ignore (if needed)
    private static List<String> ignoredTests = Arrays.asList(
    "get_factors",
		"mergesort"
		
    );

    public static CommandSummary getQuixBugsCommand(String name) {
        CommandSummary cs = new CommandSummary();
        String dep = new File("./examples/libs/junit-4.4.jar").getAbsolutePath();
        cs.command.put("-javacompliancelevel", "8");
        cs.command.put("-seed", "1");
        cs.command.put("-package", "java_programs");
        cs.command.put("-dependencies", dep);
        cs.command.put("-scope", "package");
        cs.command.put("-mode", "custom");
        cs.command.put("-id", name);
        cs.command.put("-population", "1");
        cs.command.put("-srcjavafolder", "/src");
        cs.command.put("-srctestfolder", "/src");
        cs.command.put("-binjavafolder", "/bin");
        cs.command.put("-bintestfolder", "/bin");
        cs.command.put("-flthreshold", "0.0");
        cs.command.put("-loglevel", "INFO");
        cs.command.put("-stopfirst", "TRUE");
        cs.command.put("-customengine", LLMIngredientEngine.class.getName());
        
        // Store the LLM parameters for later reporting
        llmService = "ollama";
        llmModel = "codellama:13b";
        maxSuggestionsPerPoint = 3;
        llmPromptTemplate = "MULTIPLE_SOLUTION";
        
        cs.command.put("-parameters",
                "logtestexecution" + File.pathSeparator + "TRUE" + File.pathSeparator + "" + "disablelog"
                        + File.pathSeparator + "FALSE" + File.pathSeparator + "maxtime" + File.pathSeparator + "120"
                        + File.pathSeparator + "autocompile" + File.pathSeparator + "false" + File.pathSeparator
                        + "gzoltarpackagetonotinstrument" + File.pathSeparator + "com.google.gson_engine" +
                        // + File.pathSeparator + "java_programs_test"
                        File.pathSeparator + "llmService" + File.pathSeparator + llmService +  // LLM
                        File.pathSeparator + "llmmodel" + File.pathSeparator + llmModel +  // Use CodeLlama model
                        File.pathSeparator + "maxsuggestionsperpoint" + File.pathSeparator + maxSuggestionsPerPoint + // Only generate 1 suggestion per point
                        File.pathSeparator + "llmprompttemplate" + File.pathSeparator + llmPromptTemplate // Use the detailed repair template
        );
        cs.command.put("-location", new File("./examples/quixbugscompiled/" + name).getAbsolutePath());

        return cs;
    }
    
    /**
     * Main method to run all tests and collect results
     */
    public static void main(String[] args) {
        System.out.println("Starting QuixBugs repair tests with LLM integration...");
        
        // Define timeout for each test (10 minutes)
        final long TEST_TIMEOUT = 10 * 60 * 1000; // 10 minutes in milliseconds
        
        int totalTests = 0;
        int completedTests = 0;
        
        System.out.println("Found " + quixBugsTests.size() + " QuixBugs tests to run");
        System.out.println("Ignoring " + ignoredTests.size() + " tests");
        
        // Run each test and collect results
        for (String testName : quixBugsTests) {
            // Skip ignored tests
            if (ignoredTests.contains(testName)) {
                System.out.println("\nSkipping ignored test: " + testName);
                continue;
            }
            
            totalTests++;
            
            System.out.println("\n----------------------");
            System.out.println("Running test: " + testName + " (" + completedTests + "/" + 
                               (quixBugsTests.size() - ignoredTests.size()) + ")");
            
            // Record start time
            long startTime = System.currentTimeMillis();
            
            // Flag to track if test completed
            final boolean[] testCompleted = {false};
            
            // Create separate thread for test execution
            Thread testThread = new Thread(() -> {
                try {
                    executeQuixBugsTest(testName);
                    
                    // Mark as completed and add to passed tests
                    testCompleted[0] = true;
                    passedTests.add(testName);
                    
                    // Calculate execution time
                    long executionTime = System.currentTimeMillis() - startTime;
                    testExecutionTimes.put(testName, executionTime);
                    
                    System.out.println("Test PASSED: " + testName);
                    System.out.println("Execution time: " + formatExecutionTime(executionTime));
                } catch (Exception e) {
                    // Mark as completed and add to failed tests
                    testCompleted[0] = true;
                    failedTests.add(testName);
                    
                    // Calculate execution time
                    long executionTime = System.currentTimeMillis() - startTime;
                    testExecutionTimes.put(testName, executionTime);
                    
                    System.out.println("Test FAILED: " + testName);
                    System.out.println("Execution time: " + formatExecutionTime(executionTime));
                    System.out.println("Reason: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
                }
            });
            
            // Start test execution
            testThread.start();
            
            // Wait for test to complete or timeout
            try {
                testThread.join(TEST_TIMEOUT);
                
                // Check if test is still running after timeout
                if (testThread.isAlive()) {
                    // Test exceeded time limit
                    System.out.println("TIME EXCEEDED: " + testName);
                    System.out.println("Execution time: > " + formatExecutionTime(TEST_TIMEOUT));
                    System.out.println("Moving to next test...");
                    
                    // Add to failed tests
                    failedTests.add(testName);
                    testExecutionTimes.put(testName, TEST_TIMEOUT);
                    
                    // Interrupt the test thread (best effort)
                    testThread.interrupt();
                    
                    // In case interruption doesn't work, use more aggressive approach
                    try {
                        // Give the thread a chance to respond to interruption
                        Thread.sleep(5000);
                        
                        // If still alive, use deprecated method as last resort
                        if (testThread.isAlive()) {
                            testThread.stop();
                        }
                    } catch (Exception e) {
                        // Ignore exceptions from stopping the thread
                    }
                }
            } catch (InterruptedException e) {
                System.out.println("Main thread was interrupted while waiting for test to complete");
            }
            
            completedTests++;
        }
        
        // Print summary
        printSummary();
    }
    
    /**
     * Execute a QuixBugs test with the given name
     */
    private static void executeQuixBugsTest(String testName) throws Exception {
        AstorMain main1 = new AstorMain();
        CommandSummary command = getQuixBugsCommand(testName);
        command.command.put("-maxgen", "500"); // Set max generations
        
        // For certain tests that might need more generations
        if (testName.equals("powerset") || testName.equals("lis") || testName.equals("bitcount") || 
            testName.equals("hanoi")) {
            command.command.put("-maxgen", "1000");
        }
        
        main1.execute(command.flat());
        assertTrue("No solution for " + testName, main1.getEngine().getSolutions().size() > 0);
    }
    
    /**
     * Format execution time in a human-readable format
     */
    private static String formatExecutionTime(long timeInMs) {
        if (timeInMs < 1000) {
            return timeInMs + " ms";
        } else if (timeInMs < 60000) {
            return String.format("%.2f seconds", timeInMs / 1000.0);
        } else {
            long minutes = timeInMs / 60000;
            long seconds = (timeInMs % 60000) / 1000;
            return minutes + " minutes, " + seconds + " seconds";
        }
    }
    
    /**
     * Print a summary of the test results and LLM configuration
     */
    private static void printSummary() {
        System.out.println("\n==============================================");
        System.out.println("QuixBugs Repair Test Summary with LLM Integration");
        System.out.println("==============================================");
        System.out.println("LLM Configuration:");
        System.out.println("- LLM Service: " + llmService);
        System.out.println("- LLM Model: " + llmModel);
        System.out.println("- Max Suggestions Per Point: " + maxSuggestionsPerPoint);
        System.out.println("- LLM Prompt Template: " + llmPromptTemplate);
        
        System.out.println("\nTest Results:");
        int totalTests = passedTests.size() + failedTests.size();
        System.out.println("- Total Tests Run: " + totalTests);
        System.out.println("- Passed Tests: " + passedTests.size() + " (" + 
                          Math.round((double)passedTests.size() / totalTests * 100) + "%)");
        System.out.println("- Failed Tests: " + failedTests.size() + " (" +
                          Math.round((double)failedTests.size() / totalTests * 100) + "%)");
        
        System.out.println("\nPassed Tests:");
        for (String test : passedTests) {
            System.out.println("  ✅ " + test + " (" + formatExecutionTime(testExecutionTimes.get(test)) + ")");
        }
        
        System.out.println("\nFailed Tests:");
        for (String test : failedTests) {
            System.out.println("  ❌ " + test + " (" + formatExecutionTime(testExecutionTimes.get(test)) + ")");
        }
        
        // Calculate and show total execution time
        long totalExecutionTime = testExecutionTimes.values().stream().mapToLong(Long::longValue).sum();
        System.out.println("\nTotal Execution Time: " + formatExecutionTime(totalExecutionTime));
        System.out.println("==============================================");
    }
    
    /**
     * Single test method for compatibility with JUnit
     * This method is not used when running through main()
     */
    @Test
    public void testAllQuixBugs() throws Exception {
        main(new String[0]);
    }
}