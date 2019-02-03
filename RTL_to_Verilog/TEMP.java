import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

public class TEMP {
    public static final Scanner scanner = new Scanner(System.in);

    public static ArrayList<String> RTL = new ArrayList<>();

    public static String Verilog = "";
    public static String ALU_verilog;
    public static String ALU_bus_verilog;
    public static String RS_FF_verilog;

    public static ArrayList<String> DP_registers = new ArrayList<>();
    public static ArrayList<String> DP_flipflops = new ArrayList<>();
    public static ArrayList<String> CU_Flipflops = new ArrayList<>();
    public static ArrayList<String> USER_inputs = new ArrayList<>();

    public static int REG_COUNT; //DP RELATED
    public static int REG_WIDTH = 16; //DP RELATED
    public static int BUS_WIDTH; //DP ALU INPUT BUS RELATED

    public static int ALU_OPERATION_COUNT; //ALU NUMBER OF OPERATIONS
    public static int ALU_COMMAND_WIDTH;
    public static ArrayList<String> ALU_OPERATIONS = new ArrayList<>();
    public static Map<String, String> ALU_OPERATION_TO_CASE_MAPPER = new HashMap<>();


    public static void main(String[] args) throws Exception {
        File file = new File("/Users/deyapple/Downloads/RTL_to_Verilog/RTL.txt");
        BufferedReader br = new BufferedReader(new FileReader(file));
        String st;
        while ((st = br.readLine()) != null)
            RTL.add(st);

        assign_DP_Registers();
        assign_User_inputs();
        assign_CU_Flipflops();

        design_ALU();

        ALU_bus_verilog = design_MUX("BUS_CONTROLLER", REG_COUNT, REG_WIDTH);
        RS_FF_verilog = design_RS_FF();

        System.out.println(design_comparator(REG_WIDTH));
    }

    public static void assign_DP_Registers(){
        String init_line = RTL.get(0).
                replace("~", "").
                replace(" ", "");

        String control_part = init_line.substring(0, init_line.indexOf(":"));
        ArrayList<String> dp_remove = new ArrayList<>();
        Collections.addAll(dp_remove, control_part.split("\\."));
        CU_Flipflops.addAll(dp_remove);

        String dp_part = init_line.substring(init_line.indexOf(":") + 1);
        ArrayList<String> dp_reg_candids = new ArrayList<>();

        ArrayList<String> statements = new ArrayList<>();
        Collections.addAll(statements, dp_part.split(","));
        for (String statement : statements) {
            String left = statement.substring(0, statement.indexOf("<-"));
            if (!dp_remove.contains(left)){
                dp_reg_candids.add(left);
            }
        }
        DP_registers.addAll(dp_reg_candids);
        REG_COUNT = DP_registers.size();
        BUS_WIDTH = (int) Math.ceil(Math.log(REG_COUNT) / Math.log(2));
    }

    public static void assign_User_inputs(){
        String init_line = RTL.get(0).
                replace("~", "").
                replace(" ", "");

        String dp_part = init_line.substring(init_line.indexOf(":") + 1);
        ArrayList<String> inputs = new ArrayList<>();
        ArrayList<String> statements = new ArrayList<>();
        Collections.addAll(statements, dp_part.split(","));

        for (String statement : statements) {
            String right = statement.substring(statement.indexOf("<-") + 2);
            if (!right.equals("0") && !right.equals("1")){
                inputs.add(right);
            }
        }
        USER_inputs.addAll(inputs);
    }

    public static void assign_CU_Flipflops(){
        ArrayList<String> CU_candids = new ArrayList<>();
        CU_candids.addAll(CU_Flipflops);
        CU_Flipflops.removeAll(CU_Flipflops);

        for (String line : RTL) {
            String temp = line.replace("~", "").
                    replace(" ", "");

            ArrayList<String> conditions = new ArrayList<>();
            Collections.addAll(conditions, temp.
                    substring(0, temp.indexOf(":")).split("\\."));

            for (String condition : conditions) {
                if (condition.contains("(") || condition.contains("[")){
                    continue;
                }
                if (!CU_candids.contains(condition)){
                    CU_candids.add(condition);
                }
            }
        }

        CU_Flipflops.addAll(CU_candids);
    }

    public static void assign_ALU_operations(){
        ArrayList<String> operations = new ArrayList<>();
        operations.add("transfer");
        for (String line : RTL) {
            String temp = line.replace("~", "").
                    replace(" ", "");
            temp = temp.substring(temp.indexOf(":") + 1);

            ArrayList<String> statements = new ArrayList<>();
            Collections.addAll(statements, temp.split(","));
            for (String statement : statements) {
                String left = statement.substring(0, statement.indexOf("<-"));
                if (!DP_registers.contains(left))
                    continue;

                statement = statement.substring(statement.indexOf("<-") + 2);
                if (statement.equals("1"))
                    if (!operations.contains("1"))
                        operations.add("1");
                if (statement.equals("0"))
                    if (!operations.contains("0"))
                        operations.add("0");
                if (statement.equals("-1"))
                    if (!operations.contains("-1"))
                        operations.add("-1");
                if (statement.contains("~"))
                    if (!operations.contains("~"))
                        operations.add("~");
                if (statement.contains("+"))
                    if (!operations.contains("+"))
                        operations.add("+");
                if (statement.contains("-"))
                    if (!operations.contains("-"))
                        operations.add("-");
                if (statement.contains("AND"))
                    if (!operations.contains("AND"))
                        operations.add("AND");
                if (statement.contains("OR"))
                    if (!operations.contains("OR"))
                        operations.add("OR");
                if (statement.matches("-.*"))
                    if (!operations.contains("neg"))
                        operations.add("neg");
                if (statement.matches(".* \\+ 1"))
                    if (!operations.contains("++"))
                        operations.add("++");
                if (statement.matches(".* - 1"))
                    if (!operations.contains("--"))
                        operations.add("--");
            }
        }
        ALU_OPERATIONS.addAll(operations);
        ALU_OPERATION_COUNT = ALU_OPERATIONS.size();
        ALU_COMMAND_WIDTH = (int) Math.ceil(Math.log(ALU_OPERATION_COUNT) / Math.log(2));
    }

    private static ArrayList<String> assign_cases(int count){
        ArrayList<String> cases = new ArrayList<>();
        int width = (int) Math.ceil(Math.log(count) / Math.log(2));;

        for (int i = 0; i < count; i++){
            String temp = String.valueOf(Integer.toBinaryString(i));
            if (temp.length() < count){
                String add = new String();
                int less = width - temp.length();
                for (int j = 0; j < less; j++)
                    add = add.concat("0");
                cases.add(width + "'b" + add + temp);
            } else{
                cases.add(width + "'b" + temp.substring(0, width));
            }

        }
        return cases;
    }

    private static ArrayList<String> assign_ALU_operations_lines(){
        ArrayList<String> cases = assign_cases(ALU_OPERATION_COUNT);
        ArrayList<String> lines = new ArrayList<>();
        for (int i = 0; i < ALU_OPERATION_COUNT; i++){
            String operation = ALU_OPERATIONS.get(i);
            String curr_case = cases.get(i);
            ALU_OPERATION_TO_CASE_MAPPER.put(operation, curr_case);
            switch (operation){
                case "0": lines.add(curr_case + ": Data_out = " + "0;"); break;
                case "1": lines.add(curr_case + ": Data_out = " + "1;"); break;
                case "-1": lines.add(curr_case + ": Data_out = " + "-1;"); break;
                case "~": lines.add(curr_case + ": Data_out = " + "~Data_in1;"); break;
                case "neg": lines.add(curr_case + ": Data_out = " + "-Data_in1;"); break;
                case "transfer": lines.add(curr_case + ": Data_out = " + "Data_in1;"); break;
                case "++": lines.add(curr_case + ": Data_out = " + "Data_in1 + 1;"); break;
                case "--": lines.add(curr_case + ": Data_out = " + "Data_in1 - 1;"); break;
                case "+": lines.add(curr_case + ": Data_out = " + "Data_in1 + Data_in2;"); break;
                case "-": lines.add(curr_case + ": Data_out = " + "Data_in1 - Data_in2;"); break;
                case "AND": lines.add(curr_case + ": Data_out = " + "Data_in1 & Data_in2;"); break;
                case "OR": lines.add(curr_case + ": Data_out = " + "Data_in1 | Data_in2;"); break;
            }
        }
        return lines;
    }

    public static void design_ALU(){
        assign_ALU_operations();
        ArrayList<String> case_lines = assign_ALU_operations_lines();

        String ALU = "module ALU(Data_out, Data_in1, Data_in2, control);\n\n";
        ALU = ALU.concat("\toutput reg [" + (REG_WIDTH - 1) + ":0] Data_out;\n");
        ALU = ALU.concat("\tinput [" + (REG_WIDTH - 1) + ":0] Data_in1, Data_in1;\n");
        ALU = ALU.concat("\tinput [" + (ALU_COMMAND_WIDTH - 1) + ":0] control;\n\n");

        ALU = ALU.concat("\talways @(Data_in1, Data_in2, control) begin\n");
        ALU = ALU.concat("\t\tcase(control)\n");

        for (String case_line : case_lines) {
            ALU = ALU.concat("\t\t\t" + case_line + "\n");
        }
        ALU = ALU.concat("\t\t\tdefault: Data_out = 0;\n");

        ALU = ALU.concat("\t\tendcase \n");
        ALU = ALU.concat("\tend\n\n");
        ALU = ALU.concat("endmodule");

        ALU_verilog = ALU;
    }

    public static String design_MUX(String name, int input_count, int input_width){
        String MUX = "module " + name + "(";

        /*** INPUT OUTPUT ASSIGNMENT **/
        for (int i = 0; i < input_count; i++){
            MUX = MUX.concat("I" + i + ", ");
        }
        MUX = MUX.concat("sel, out);\n\n");
        MUX = MUX.concat("\toutput reg [" + (input_width - 1) + ":0] out;\n");
        int sel_width = BUS_WIDTH = (int) Math.ceil(Math.log(input_count) / Math.log(2));
        MUX = MUX.concat("\tinput [" + (sel_width - 1) + ":0] " + "sel, \n");
        for (int i = 0; i < input_count; i++){
            MUX = MUX.concat("\tinput [" + (input_width - 1) + ":0] " + "I" + i + ";\n");
        }
        MUX = MUX.concat("\n");

        MUX = MUX.concat("\talways @(*) begin\n");
        MUX = MUX.concat("\t\tcase(sel)\n");
        ArrayList<String> sel_cases = assign_cases(input_count);
        int i = 0;
        for (String sel_case : sel_cases) {
            MUX = MUX.concat("\t\t\t" + sel_case + ": out = I" + i + ";\n");
            i += 1;
        }
        MUX = MUX.concat("\t\tendcase \n");
        MUX = MUX.concat("\tend\n\n");
        MUX = MUX.concat("endmodule");
        return MUX;
    }

    public static String design_RS_FF(){
        String RS_FF = "module RS_FF(clk, R, S, Q, Q_bar);\n\n";

        RS_FF += "\toutput reg Q, Q_bar;\n";
        RS_FF += "\tinput R, S, clk;\n\n";

        RS_FF += "\talways @(posedge clk) begin\n";
        RS_FF += "\t\tcase ({S, R})\n";
        RS_FF += "\t\t\t{1'b0,1'b0}: Q <= Q, Q_bar <= Q_bar;\n";
        RS_FF += "\t\t\t{1'b0,1'b1}: Q <= 1'b0; Q_bar <= 1'b1;\n";
        RS_FF += "\t\t\t{1'b1,1'b0}: Q <= 1'b1; Q_bar <= 1'b0;\n";
        RS_FF += "\t\t\t{1'b1,1'b1}: Q <= Q_bar, Q_bar <= Q;\n";
        RS_FF += "\t\tendcase\n";
        RS_FF += "\tend\n\n";
        RS_FF += "endmodule";
        return RS_FF;
    }

    public static String design_comparator(int input_width){
        String CMP = "module CMP(A, B, AGB, ALB, AEB);\n\n";

        CMP += "\toutput AGB, ALB, AEB;\n";
        CMP += "\tinput [" + (input_width - 1) + ":0] A, B;\n\n";

        CMP += "\tassign AGB = (A > B);\n";
        CMP += "\tassign ALB = (A < B);\n";
        CMP += "\tassign AEB = (A == B);\n\n";

        CMP += "endmodule";

        return CMP;
    }
}
