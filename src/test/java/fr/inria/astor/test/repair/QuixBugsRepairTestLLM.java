package fr.inria.astor.test.repair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
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
        maxSuggestionsPerPoint = 1;
        llmPromptTemplate = "MULTIPLE_SOLUTIONS";
        
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
        QuixBugsRepairTestLLM testRunner = new QuixBugsRepairTestLLM();
        
        // Get all test methods from this class
        Method[] methods = QuixBugsRepairTestLLM.class.getDeclaredMethods();
        List<Method> testMethods = new ArrayList<>();
        
        // Filter for test methods that repair Quixbug problems
        for (Method method : methods) {
            if (method.isAnnotationPresent(Test.class) && 
                !method.isAnnotationPresent(Ignore.class) && 
                method.getName().contains("Repair")) {
                testMethods.add(method);
            }
        }
        
        System.out.println("Found " + testMethods.size() + " QuixBugs repair tests to run");
        
        // Run each test and collect results
        for (Method testMethod : testMethods) {
            String testName = testMethod.getName();
            System.out.println("\n----------------------");
            System.out.println("Running test: " + testName);
            
            // Record start time
            long startTime = System.currentTimeMillis();
            
            try {
                testMethod.invoke(testRunner);
                passedTests.add(testName);
                
                // Calculate and store execution time
                long executionTime = System.currentTimeMillis() - startTime;
                testExecutionTimes.put(testName, executionTime);
                
                System.out.println("Test PASSED: " + testName);
                System.out.println("Execution time: " + formatExecutionTime(executionTime));
            } catch (Exception e) {
                // Calculate and store execution time even for failures
                long executionTime = System.currentTimeMillis() - startTime;
                testExecutionTimes.put(testName, executionTime);
                
                failedTests.add(testName);
                System.out.println("Test FAILED: " + testName);
                System.out.println("Execution time: " + formatExecutionTime(executionTime));
                System.out.println("Reason: " + e.getCause().getMessage());
            }
        }
        
        // Print summary
        printSummary();
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
        System.out.println("- Total Tests: " + (passedTests.size() + failedTests.size()));
        System.out.println("- Passed Tests: " + passedTests.size());
        System.out.println("- Failed Tests: " + failedTests.size());
        
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
	 * Repaired in paper
	 * 
	 * @throws Exception
	 */
	@Test
	public void test_shortest_path_lengthsRepair() throws Exception {
		AstorMain main1 = new AstorMain();

		CommandSummary command = (getQuixBugsCommand("shortest_path_lengths"));
		command.command.put("-maxgen", "500");
		main1.execute(command.flat());

		assertTrue("No solution", main1.getEngine().getSolutions().size() > 0);
		

	}

	/**
	 * Repaired in paper
	 * 
	 * @throws Exception
	 */
	//@Test(timeout=10)
	@Test
	public void test_depth_first_searchRepair() throws Exception {
		AstorMain main1 = new AstorMain();

		CommandSummary command = (getQuixBugsCommand("depth_first_search"));
		command.command.put("-maxgen", "500");
		main1.execute(command.flat());

		assertTrue("No solution", main1.getEngine().getSolutions().size() > 0);

	}

	/**
	 * Repaired in paper
	 * 
	 * @throws Exception
	 */
	@Test
	public void test_next_permutationRepair() throws Exception {
		AstorMain main1 = new AstorMain();

		CommandSummary command = (getQuixBugsCommand("next_permutation"));
		command.command.put("-maxgen", "500");
		main1.execute(command.flat());

		assertTrue("No solution", main1.getEngine().getSolutions().size() > 0);

	}

	/**
	 * Repaired in paper
	 * 
	 * @throws Exception
	 */
	@Test
	public void test_next_knapsackRepair() throws Exception {
		AstorMain main1 = new AstorMain();

		CommandSummary command = (getQuixBugsCommand("knapsack"));
		command.command.put("-maxgen", "500");
		main1.execute(command.flat());

		assertTrue("No solution", main1.getEngine().getSolutions().size() > 0);

	}

	/**
	 * Repaired in paper
	 * 
	 * @throws Exception
	 */
	@Test
	public void test_quicksortRepair() throws Exception {
		AstorMain main1 = new AstorMain();

		CommandSummary command = (getQuixBugsCommand("quicksort"));
		command.command.put("-maxgen", "500");
		main1.execute(command.flat());

		assertTrue("No solution", main1.getEngine().getSolutions().size() > 0);

	}

	/**
	 * Repaired in paper
	 * 
	 * @throws Exception
	 */
	@Test
	public void test_rpn_evalRepair() throws Exception {
		AstorMain main1 = new AstorMain();

		CommandSummary command = (getQuixBugsCommand("rpn_eval"));
		command.command.put("-maxgen", "500");
		main1.execute(command.flat());

		assertTrue("No solution", main1.getEngine().getSolutions().size() > 0);

	}

	/**
	 * Repaired in paper
	 * 
	 * @throws Exception
	 */
	@Test
	public void test_levenshteinRepair() throws Exception {
		AstorMain main1 = new AstorMain();

		CommandSummary command = (getQuixBugsCommand("levenshtein"));
		command.command.put("-maxgen", "500");
		main1.execute(command.flat());

		assertTrue("No solution", main1.getEngine().getSolutions().size() > 0);

	}

	/**
	 * Repaired in paper
	 * 
	 * @throws Exception
	 */
	@Test
	@Ignore
	public void test_get_factorsRepair() throws Exception {
		AstorMain main1 = new AstorMain();

		CommandSummary command = (getQuixBugsCommand("get_factors"));
		command.command.put("-maxgen", "500");
		main1.execute(command.flat());

		assertTrue("No solution", main1.getEngine().getSolutions().size() > 0);

	}

	/**
	 * Repaired in paper
	 * 
	 * @throws Exception
	 */
	@Test
	public void test_mergesortRepair() throws Exception {
		AstorMain main1 = new AstorMain();

		CommandSummary command = (getQuixBugsCommand("mergesort"));
		command.command.put("-maxgen", "500");// do not evolve right now
		main1.execute(command.flat());

		assertTrue("No solution", main1.getEngine().getSolutions().size() > 0);
	}

	/**
	 * Repaired in paper
	 * 
	 * @throws Exception
	 */
	@Test
	public void test_powersetRepair() throws Exception {
		AstorMain main1 = new AstorMain();

		CommandSummary command = (getQuixBugsCommand("powerset"));
		command.command.put("-maxgen", "1000");
		main1.execute(command.flat());

		assertTrue("No solution", main1.getEngine().getSolutions().size() > 0);
	}

	/**
	 * Repaired in paper
	 * 
	 * @throws Exception
	 */
	@Test
	public void test_lisRepair() throws Exception {
		AstorMain main1 = new AstorMain();

		CommandSummary command = (getQuixBugsCommand("lis"));
		command.command.put("-maxgen", "1000");
		main1.execute(command.flat());

		assertTrue("No solution", main1.getEngine().getSolutions().size() > 0);
	}

	/**
	 * 
	 * 
	 * @throws Exception
	 */
	@Test
	public void ne_test_hanoiRepair() throws Exception {
		AstorMain main1 = new AstorMain();

		CommandSummary command = (getQuixBugsCommand("hanoi"));
		command.command.put("-maxgen", "1000");
		main1.execute(command.flat());

		assertTrue("No solution", main1.getEngine().getSolutions().size() > 0);

	}

}