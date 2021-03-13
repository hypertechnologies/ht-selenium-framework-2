package framework.main;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.io.*;
import java.util.List;

import static framework.main.Keywords.closeBrowser;

public class Main extends Base {
    public static boolean tc_failed = false;
    private static String testCaseFilePath;
    private static String sessionId;

    public static void main(String[] args) throws InterruptedException, IOException {
        suiteConfigs = getSuiteConfigs();
        sessionId = getCurrentDateTime();
        JSONArray suite = getSuite();
        for (Object browser : getBrowsers()) {
            runTestSuite(suite,  browser.toString());
        }
    }

    private static void runTestSuite(JSONArray suite, String browser) {
        for (Object o : suite) {
            JSONObject testCaseObj = (JSONObject) o;
            String testCaseDir = (String) testCaseObj.get("directory");
            Boolean testCaseDirSkip = (Boolean) testCaseObj.get("skip");
            List<File> testCasePaths = getAllTestCaseFilePaths(testCaseDir);

            if(!testCaseDirSkip){
                for (File path : testCasePaths) {
                    testCaseFilePath = path.toString();
                    runTestScenarios(browser);
                }
            }
        }
    }

    private static void runTestScenarios(String browser) {
        // Getting indexes for test scenario sheet
        String tsSheetName = (String) getExcelIndexes().get("tsSheetName");
        int tsIdColumnIndex = (int) (long) getExcelIndexes().get("tsIdColumnIndex");
        int tsSkipColumnIndex = (int) (long) getExcelIndexes().get("tsSkipColumnIndex");

        XSSFWorkbook workbook = getWorkbook(testCaseFilePath);

        // Getting Scenarios Sheet
        assert workbook != null;
        XSSFSheet tsSheet = workbook.getSheet(tsSheetName);

        // Reading info from Test Scenario sheet
        for (int i = 1; i < tsSheet.getLastRowNum() + 1; i++){
            tc_failed = false;

            Cell tsIdCell = tsSheet.getRow(i).getCell(tsIdColumnIndex);
            Cell tsSkipCell = tsSheet.getRow(i).getCell(tsSkipColumnIndex);

            if(tsIdCell == null || tsIdCell.toString().equalsIgnoreCase("")){
                break;
            }

            DataFormatter formatter = new DataFormatter();
            int tsId = Integer.parseInt(formatter.formatCellValue(tsIdCell));
            boolean tsSkip = Boolean.parseBoolean(formatter.formatCellValue(tsSkipCell));

            // !TS_skip = TS_skip is not true or TS_skip is false
            if(!tsSkip){
                Keywords.openBrowser(browser);
                runTestCases(tsId, workbook);
                closeBrowser();
            }
        }
        closeWorkBook(workbook);
    }

    private static void runTestCases(int tsId, XSSFWorkbook workbook) {
        String tcSheetNamePrefix = (String) getExcelIndexes().get("tcSheetNamePrefix");

        // Getting TC sheet
        XSSFSheet tcSheet = workbook.getSheet(tcSheetNamePrefix + tsId);

        // Getting indexes for Test Case sheet
        int tcKeywordColumnIndex = (int) (long) getExcelIndexes().get("tcKeywordColumnIndex");
        int tcSelectorTypeColumnIndex = (int) (long) getExcelIndexes().get("tcSelectorTypeColumnIndex");
        int tcSelectorValueColumnIndex = (int) (long) getExcelIndexes().get("tcSelectorValueColumnIndex");
        int tcTestDataColumnIndex = (int) (long) getExcelIndexes().get("tcTestDataColumnIndex");
        int tcResultColumnIndex = (int) (long) getExcelIndexes().get("tcResultColumnIndex");
        int tcCommentColumnIndex = (int) (long) getExcelIndexes().get("tcCommentColumnIndex");
        int tcSkipIndex = (int) (long) getExcelIndexes().get("tcSkipIndex");

        DataFormatter formatter = new DataFormatter();
        for(int j = 1; j < tcSheet.getLastRowNum() + 1; j++){
            Cell tcKeyword = tcSheet.getRow(j).getCell(tcKeywordColumnIndex);

            if(tcKeyword == null || tcKeyword.toString().equalsIgnoreCase("") || tc_failed){
                break;
            }
            // Convert all cell values to string
            String keyword = formatter.formatCellValue(tcKeyword);
            String selectorType = formatter.formatCellValue(tcSheet.getRow(j).getCell(tcSelectorTypeColumnIndex));
            String selectorValue = formatter.formatCellValue(tcSheet.getRow(j).getCell(tcSelectorValueColumnIndex));
            String testData = formatter.formatCellValue(tcSheet.getRow(j).getCell(tcTestDataColumnIndex));
            boolean skip = Boolean.parseBoolean(formatter.formatCellValue(tcSheet.getRow(j).getCell(tcSkipIndex)));

            if(!skip){
                runTestSteps(keyword, selectorType, selectorValue, testData, j, tcResultColumnIndex, tcCommentColumnIndex, tcSheet);
            }
        }
        saveResultFile(workbook, testCaseFilePath, sessionId);
    }

    private static void runTestSteps(String keyword, String selectorType, String selectorValue, String testData, int row, int tcResultColumnIndex, int tcCommentColumnIndex, XSSFSheet tcSheet) {
        switch (keyword.trim().toLowerCase()){
            case "gotourl":
                Keywords.gotToURL(testData, row, tcResultColumnIndex, tcCommentColumnIndex, tcSheet);
                break;
            case "type":
                Keywords.type(selectorType, selectorValue, testData, row, tcResultColumnIndex, tcCommentColumnIndex, tcSheet);
                break;
            case "click":
                Keywords.click(selectorType, selectorValue, row, tcResultColumnIndex, tcCommentColumnIndex, tcSheet);
                break;
            case "asserttext":
                Keywords.assertText(selectorType, selectorValue, testData, row, tcResultColumnIndex, tcCommentColumnIndex, tcSheet);
                break;
            case "asserttitle":
                Keywords.assertTitle(testData, row, tcResultColumnIndex, tcCommentColumnIndex, tcSheet);
                break;
            case "refreshpage":
                Keywords.refreshPage(row, tcResultColumnIndex, tcCommentColumnIndex, tcSheet);
                break;
            case "checkvisibility":
                Keywords.checkVisibility(selectorType, selectorValue, row, tcResultColumnIndex, tcCommentColumnIndex, tcSheet);
                break;
            case "checknotvisible":
                Keywords.checkNotVisible(selectorType, selectorValue, row, tcResultColumnIndex, tcCommentColumnIndex, tcSheet);
                break;
            case "selectbyvisibletext":
                Keywords.selectByVisibleText(selectorType, selectorValue, testData, row, tcResultColumnIndex, tcCommentColumnIndex, tcSheet);
                break;
            case "selectbyindex":
                Keywords.selectByIndex(selectorType, selectorValue, testData, row, tcResultColumnIndex, tcCommentColumnIndex, tcSheet);
                break;
            case "enablecheckbox":
                Keywords.enableCheckBox(selectorType, selectorValue, row, tcResultColumnIndex, tcCommentColumnIndex, tcSheet);
                break;
            case "disablecheckbox":
                Keywords.disableCheckBox(selectorType, selectorValue, row, tcResultColumnIndex, tcCommentColumnIndex, tcSheet);
                break;
            case "asserturl":
                Keywords.assertURL(testData, row, tcResultColumnIndex, tcCommentColumnIndex, tcSheet);
                break;
            case "closebrowser":
                closeBrowser();
                break;
            default:
                System.out.println("Keyword named \"" + keyword + "\" is not recognized!");
                break;
        }
    }


}