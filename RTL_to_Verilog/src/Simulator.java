import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.*;
import java.io.*;
import java.util.*;

public class Simulator extends Application{

    private static ArrayList<String> RTL = new ArrayList<>();

    private static String Verilog = "";

    private static Map<String, Boolean> DP_parts = new HashMap<>(); // TRUE = Register, FALSE = FF
    private static Map<String, Integer> DP_registers = new HashMap<>();
    private static ArrayList<String> DP_flipflops = new ArrayList<>();
    private static ArrayList<String> CU_Flipflops = new ArrayList<>();
    private static Map<String, Integer> USER_inputs = new HashMap<>();
    private static Map<String, Integer> outputs = new HashMap<>();


    public static void main(String[] args) throws Exception {
        launch(args);
    }

    private void assign(){
        assign_DP_parts();
        assign_DP_reg_FF();
        assign_CU_Flipflops();
        assign_User_inputs();
        assign_outputs();
        assign_widths();
    }

    private static void assign_DP_parts() {
        // first line should be initialization
        String init_line = RTL.get(0).
                replace("~", "").
                replace(" ", "");

        String control_part = init_line.substring(0, init_line.indexOf(":"));
        ArrayList<String> dp_remove = new ArrayList<>();
        Collections.addAll(dp_remove, control_part.split("\\."));
        CU_Flipflops.addAll(dp_remove);

        String dp_part = init_line.substring(init_line.indexOf(":") + 1);

        ArrayList<String> statements = new ArrayList<>();
        Collections.addAll(statements, dp_part.split(","));
        for (String statement : statements) {
            String left = statement.substring(0, statement.indexOf("<-"));
            if (!dp_remove.contains(left)) {
                DP_parts.put(left, Boolean.FALSE);
            }
        }
    }

    private static void assign_User_inputs() {
        String init_line = RTL.get(0).
                replace("~", "").
                replace(" ", "");

        String dp_part = init_line.substring(init_line.indexOf(":") + 1);
        ArrayList<String> statements = new ArrayList<>();
        Collections.addAll(statements, dp_part.split(","));

        for (String statement : statements) {
            String right = statement.substring(statement.indexOf("<-") + 2);
            if (!right.equals("0") && !right.equals("1")) {
                USER_inputs.put(right, 0);
            }
        }
    }

    private static void assign_CU_Flipflops() {
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
                if (condition.contains("(") || condition.contains("[")) {
                    continue;
                }
                if (!CU_candids.contains(condition)) {
                    CU_candids.add(condition);
                }
            }
        }

        CU_Flipflops.addAll(CU_candids);
    }

    private static void assign_DP_reg_FF() {
        for (String line : RTL) {
            String dp_part = line.substring(line.indexOf(":") + 1).
                    replace("~", "").
                    replace(" ", "");
            ;
            ArrayList<String> statements = new ArrayList<>();
            Collections.addAll(statements, dp_part.split(","));
            for (String statement : statements) {
                String right = statement.substring(statement.indexOf("<-") + 2);
                String left = statement.substring(0, statement.indexOf("<-"));
                if (DP_parts.containsKey(left)) {
                    if (!right.equals("0") && !right.equals("1")) {
                        DP_parts.replace(left, Boolean.TRUE);
                    }
                }
            }
        }
        for (String key : DP_parts.keySet()) {
            if (DP_parts.get(key))
                DP_registers.put(key, 0);
            else
                DP_flipflops.add(key);

        }
    }

    private static void assign_outputs() {
        String fin_line = RTL.get(RTL.size() - 1).
                replace("~", "").
                replace(" ", "");
        String dp_part = fin_line.substring(fin_line.indexOf(":") + 1);
        ArrayList<String> statements = new ArrayList<>();
        Collections.addAll(statements, dp_part.split(","));
        for (String statement : statements) {
            String left = statement.substring(0, statement.indexOf("<-"));
            if (DP_registers.keySet().contains(left)) {
                outputs.put(left, 0);
                DP_registers.remove(left);
            }
            if (DP_flipflops.contains(left)){
                outputs.put(left, 1);
                DP_flipflops.remove(left);
            }
        }
    }

    private void assign_widths() {
        inputs.getChildren().clear();

        Text widths = new Text("Please enter the required widths.");
        widths.setFont(Font.font("Serif", FontWeight.NORMAL, 25));
        Text widths_default = new Text("(Default set to 16 bits.)");
        widths_default.setFont(Font.font("Serif", FontWeight.NORMAL, 17.5));
        widths_default.setLayoutY(25);
        Text widths_subtitle = new Text("When finished, press button.");
        widths_subtitle.setFont(Font.font("Serif", FontWeight.NORMAL, 20));
        widths_subtitle.setLayoutY(47.5);
        widths.setFill(Color.WHITESMOKE);
        widths_default.setFill(Color.WHITESMOKE);
        widths_subtitle.setFill(Color.WHITESMOKE);
        inputs.getChildren().addAll(widths, widths_default, widths_subtitle);

        int i = 0;
        for (String Reg : DP_registers.keySet()) {
            Text name = new Text(Reg + ":");
            name.setFont(Font.font("Serif", FontWeight.NORMAL, 20));
            name.setLayoutY(120 + 60 * i);
            name.setFill(Color.WHITESMOKE);

            TextField width = new TextField();
            width.setLayoutX(50);
            width.setLayoutY(100 + 60 * i);
            width.setStyle("-fx-control-inner-background:rgba(0,0,0,0.50); -fx-highlight-text-fill:rgba(0,0,0,0.50);");
            width.setText("16");

            MyButton done = new MyButton(new Image(getClass()
                    .getResource( "res/done.png").toExternalForm()));
            done.setOnMouseClicked(event -> DP_registers.replace(Reg, Integer.parseInt(width.getText())));
            done.setLayoutX(255);
            done.setLayoutY(90 + 60 * i);

            inputs.getChildren().addAll(name, width, done);
            i++;
        }

        MyButton convert = new MyButton(new Image(getClass()
                .getResource( "res/convert.png").toExternalForm()));
        convert.setLayoutY(25);
        convert.setLayoutX(350);
        convert.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                for (String Reg : DP_registers.keySet()) {
                    if (DP_registers.get(Reg) == 0){
                        DP_registers.replace(Reg, 16);
                    }
                }
                String init_line = RTL.get(0).
                        replace("~", "").
                        replace(" ", "");

                String dp_part_init = init_line.substring(init_line.indexOf(":") + 1);
                ArrayList<String> statements_init = new ArrayList<>();
                Collections.addAll(statements_init, dp_part_init.split(","));
                for (String statement : statements_init) {
                    String left = statement.substring(0, statement.indexOf("<-"));
                    String right = statement.substring(statement.indexOf("<-") + 2);
                    if (USER_inputs.keySet().contains(right)){
                        int width = DP_registers.get(left);
                        USER_inputs.replace(right, width);
                    }
                }

                String fin_line = RTL.get(RTL.size() - 1).
                        replace("~", "").
                        replace(" ", "");
                String dp_part_fin = fin_line.substring(fin_line.indexOf(":") + 1);
                ArrayList<String> statements_fin = new ArrayList<>();
                Collections.addAll(statements_fin, dp_part_fin.split(","));
                for (String statement : statements_fin) {
                    String left = statement.substring(0, statement.indexOf("<-"));
                    String right = statement.substring(statement.indexOf("<-") + 2);
                    if (outputs.keySet().contains(left)){
                        if (outputs.get(left) == 0){
                            int width = DP_registers.get(right);
                            outputs.replace(left, width);
                        }
                    }
                }

                Verilog = "";
                convert_to_verilog();
                Verilog += "endmodule";
                verilog_code.setText(Verilog);
                verilog_code.setVisible(true);
                lbl_verilog.setVisible(true);
                save_lbl.setVisible(true);
                save.setVisible(true);
            }
        });
        inputs.getChildren().add(convert);

    }

    private void convert_to_verilog(){
        module_declaration();
        variable_assignment();
        initial_block();
        always_block();
    }

    private static void module_declaration() {
        StringBuilder module = new StringBuilder("module RTL_to_Verilog (");
        for (String s : USER_inputs.keySet()) {
            module.append(s).append(", ");
        }
        if (CU_Flipflops.contains("S")) {
            module.append("S" + ", ");
        }
        for (String s : outputs.keySet()) {
            module.append(s).append(", ");
        }
        module.append("clk, rst);\n\n");

        Verilog += module;
    }

    private static void variable_assignment() {
        String assignment = "";

        String inputs = "\t\\\\ USER INPUTS\n";
        String outputs = "\t\\\\ OUTPUTS\n";
        String DP_regs = "";
        String DP_FFs = "";
        String CU_FFs = "\t\\\\ CONTROL UNIT\n";

        /// ASSIGN INPUTS ///
        if (USER_inputs.isEmpty()){
            inputs = "";
        }else{
            inputs += "\tinput clk, rst";
            if (CU_Flipflops.contains("S")) {
                inputs += ", S";
            }
            inputs += ";\n";
            Map<Integer, ArrayList<String>> inputs_same_width = new TreeMap<>();
            for (Map.Entry<String, Integer> entry : USER_inputs.entrySet()) {
                if (!inputs_same_width.containsKey(entry.getValue())) {
                    inputs_same_width.put(entry.getValue(), new ArrayList<>());
                }
                ArrayList<String> keys = inputs_same_width.get(entry.getValue());
                keys.add(entry.getKey());
                inputs_same_width.put(entry.getValue(), keys);
            }
            inputs += give_assignment("\t", "input", inputs_same_width);
            inputs += "\n";
        }

        /// ASSIGN OUTPUTS
        if (outputs.isEmpty()){
            outputs = "";
        }else{
            Map<Integer, ArrayList<String>> outputs_same_width = new TreeMap<>();
            for (Map.Entry<String, Integer> entry : Simulator.outputs.entrySet()) {
                if (!outputs_same_width.containsKey(entry.getValue())) {
                    outputs_same_width.put(entry.getValue(), new ArrayList<>());
                }
                ArrayList<String> keys = outputs_same_width.get(entry.getValue());
                keys.add(entry.getKey());
                outputs_same_width.put(entry.getValue(), keys);
            }
            outputs += give_assignment("\t", "output reg", outputs_same_width);
            outputs += "\n";
        }

        // ASSIGN DP REGS //
        if (DP_registers.isEmpty()){
            DP_regs = "";
        }else{
            Map<Integer, ArrayList<String>> DP_regs_same_width = new TreeMap<>();
            for (Map.Entry<String, Integer> entry : DP_registers.entrySet()) {
                if (!DP_regs_same_width.containsKey(entry.getValue())) {
                    DP_regs_same_width.put(entry.getValue(), new ArrayList<>());
                }
                ArrayList<String> keys = DP_regs_same_width.get(entry.getValue());
                keys.add(entry.getKey());
                DP_regs_same_width.put(entry.getValue(), keys);
            }
            DP_regs += give_assignment("\t", "reg", DP_regs_same_width);
            DP_regs += "\n";
        }

        // ASSIGN DP FFs //
        if (DP_flipflops.isEmpty()){
            DP_FFs = "";
        } else {
            DP_FFs += "\treg ";
            for (String dp_flipflop : DP_flipflops) {
                DP_FFs += dp_flipflop + ", ";
            }
            DP_FFs = DP_FFs.substring(0, DP_FFs.length() - 2);
            DP_FFs += ";\n";
            DP_FFs += "\n";
        }

        if (!DP_regs.isEmpty()){
            DP_regs = "\t\\\\ DATA PATH\n" + DP_regs;
        } else if (!DP_FFs.isEmpty()){
            DP_FFs = "\t\\\\ DATA PATH\n" + DP_FFs;
        }

        // ASSIGN CU FFs //
        if (CU_Flipflops.isEmpty()){
            CU_FFs = "";
        } else {
            CU_FFs += "\treg ";
            for (String cu_flipflop : CU_Flipflops) {
                CU_FFs += cu_flipflop + ", ";
            }
            CU_FFs = CU_FFs.substring(0, CU_FFs.length() - 2);
            CU_FFs += ";\n";
            CU_FFs += "\n";
        }

        assignment = inputs + outputs + CU_FFs + DP_FFs + DP_regs;
        Verilog += assignment;
    }

    private static String give_assignment(String indent, String var_type, Map<Integer, ArrayList<String>> groups) {
        String res = "";
        for (Integer width : groups.keySet()) {
            if (width != 1) {
                res += indent + var_type + " [" + (width - 1) + ":0] ";
            } else {
                res += indent + var_type + " ";
            }
            for (String register : groups.get(width)) {
                res += register + ", ";
            }
            res = res.substring(0, res.length() - 2);
            res += ";\n";
        }
        return res;
    }

    private static void always_block() {
        String always = "\talways @(posedge clk or posedge rst) begin\n";

        always += "\t\tif (rst) begin\n";
        for (String dp_flipflop : DP_flipflops) {
            always += "\t\t\t" + dp_flipflop + " = 0;\n";
        }
        for (String register : DP_registers.keySet()) {
            always += "\t\t\t" + register + " = 0;\n";
        }
        for (String cu_flipflop : CU_Flipflops) {
            always += "\t\t\t" + cu_flipflop + " = 0;\n";
        }
        always += "\t\tend\n";
        always += "\t\telse begin\n";

        for (String line : RTL) {
            String tasks = "\t\t\t\t" + line.substring(line.indexOf(":") + 1).
                    replace(",", ";\n\t\t\t\t").
                    replace("<-", "<=");
            String conditions = give_condition(line.substring(0, line.indexOf(":")));
            always += "\t\t\tif (" + conditions + ") begin\n";
            always += tasks + ";\n";
            always += "\t\t\tend\n";
        }
        always += "\t\tend\n";
        always += "\tend\n\n";

        Verilog += always;
    }

    private static String give_condition(String RTL_conditions) {
        ArrayList<String> conditions = new ArrayList<>();
        Collections.addAll(conditions, RTL_conditions.
                replace(" ", "").
                replace("+", " || ").
                split("\\."));

        String res = "";
        for (String condition : conditions) {
            if (condition.matches("L\\([a-zA-Z][a-zA-Z0-9_]*,[a-zA-Z][a-zA-Z0-9_]*\\)")) {
                res += condition.replace("L(", "").
                        replace(")", "").
                        replace(",", " < ") + " && ";
            } else if (condition.matches("G\\([a-zA-Z][a-zA-Z0-9_]*,[a-zA-Z][a-zA-Z0-9_]*\\)")) {
                res += condition.replace("G(", "").
                        replace(")", "").
                        replace(",", " > ") + " && ";
            } else if (condition.matches("E\\([a-zA-Z][a-zA-Z0-9_]*,[a-zA-Z][a-zA-Z0-9_]*\\)")) {
                res += condition.replace("E(", "").
                        replace(")", "").
                        replace(",", " == ") + " && ";
            } else if (condition.matches("~L\\([a-zA-Z][a-zA-Z0-9_]*,[a-zA-Z][a-zA-Z0-9_]*\\)")) {
                res += condition.replace("~L(", "").
                        replace(")", "").
                        replace(",", " >= ") + " && ";
            } else if (condition.matches("~G\\([a-zA-Z][a-zA-Z0-9_]*,[a-zA-Z][a-zA-Z0-9_]*\\)")) {
                res += condition.replace("~G(", "").
                        replace(")", "").
                        replace(",", " <= ") + " && ";
            } else if (condition.matches("~E\\([a-zA-Z][a-zA-Z0-9_]*,[a-zA-Z][a-zA-Z0-9_]*\\)")) {
                res += condition.replace("!E(", "").
                        replace(")", "").
                        replace(",", " != ") + " && ";
            } else if (condition.matches("OR\\([a-zA-Z][a-zA-Z0-9_]*\\)")) {
                res += condition.replace("OR(", "|").
                        replace(")", "") + " && ";
            } else if (condition.matches("~OR\\([a-zA-Z][a-zA-Z0-9_]*\\)")) {
                res += condition.replace("~OR(", "~|").
                        replace(")", "") + " && ";
            } else {
                res += condition + " && ";
            }
        }


        return res.substring(0, res.length() - 4);
    }

    private static void initial_block(){
        String initial = "\tinitial begin\n";

        String init_line = RTL.get(0).
                replace(" ", "");
        String control_part = init_line.substring(0, init_line.indexOf(":"));
        ArrayList<String> initialize = new ArrayList<>();
        Collections.addAll(initialize, control_part.split("\\."));

        for (String s : initialize) {
            if (!s.equals("S")){
                if (s.contains("~")){
                    String reg_name = s.replace("~", "");
                    initial += "\t\t" + reg_name + " = 0;\n";
                }else{
                    initial += "\t\t" + s + " = 1;\n";
                }
            }
        }
        initial += "\tend\n\n";
        Verilog += initial;
    }

    static Stage stage;

    static Scene menuScene = new Scene(new Group(), 450, 500);
    static Scene mainScene = new Scene(new Group(), 2000, 2000);
    static Pane inputs = new Pane();
    static TextArea verilog_code;
    static MyText lbl_verilog;
    static MyText save_lbl;
    static MyButton save;

    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;
        menuScene.setRoot(createMenuContent());
        stage.setScene(menuScene);

        mainScene.setRoot(createMainStageContent());

        stage.show();
        stage.centerOnScreen();
    }

    private Parent createMenuContent(){
        Group root = new Group();

        Rectangle background = new Rectangle(450, 500);

        Rectangle title_border = new Rectangle(350, 100);
        title_border.setStroke(Color.rgb(38, 139, 176));
        MyText title_text = new MyText("RTL to Verilog\n Converter", 40);
        title_text.setTextAlignment(TextAlignment.CENTER);
        StackPane title = new StackPane(title_border, title_text);
        title.setTranslateX(50);
        title.setTranslateY(50);

        VBox menubox = new VBox(40);
        menubox.setAlignment(Pos.TOP_CENTER);
        MenuItem start = new MenuItem("Start");
        start.setOnMouseClicked(event -> stage.setScene(mainScene));

        MenuItem help = new MenuItem("Help");
        help.setOnMouseClicked(event -> {
            if (Desktop.isDesktopSupported()) {
                try {
                    File myFile = new File("/Users/deyapple/Downloads/RTL_to_Verilog/documentation.pdf");
                    Desktop.getDesktop().open(myFile);
                } catch (IOException ex) {
                    // no application registered for PDFs
                }
            }
        });

        MenuItem exit = new MenuItem("Exit");
        exit.setOnMouseClicked(event -> System.exit(0));
        menubox.setTranslateX(150);
        menubox.setTranslateY(175);
        menubox.getChildren().addAll(start, help, exit);

        MyText signiture = new MyText("By Shabnam Sheikhha", 15);
        signiture.setOpacity(0.3);
        MyText email = new MyText("sheikhha@ce.sharif.edu", 15);
        email.setOpacity(0.3);
        VBox trademark = new VBox(signiture, email);
        trademark.setTranslateX(30);
        trademark.setTranslateY(450);

        root.getChildren().addAll(background, title, menubox, trademark);
        return root;
    }

    private Parent createMainStageContent(){
        Group root = new Group();

        Rectangle bg = new Rectangle(2000, 2000);
        root.getChildren().add(bg);

        MyText lbl_rtl = new MyText("Please enter the RTL code below.", 25, 50, 10);
        MyText or = new MyText("Or choose a file: ", 25, 50, 50);
        MyText lbl_rtl_subtitle = new MyText("When finished, press button.", 20, 50, 95);

        FileChooser fileChooser = new FileChooser();
        final File[] my_tmp_file = new File[1];
        MyButton browse = new MyButton(new Image(getClass()
                .getResource( "res/browse.png").toExternalForm()), 40, 40);
        browse.setOnMouseEntered(event -> mainScene.setCursor(Cursor.HAND));
        browse.setOnMouseExited(event -> mainScene.setCursor(Cursor.DEFAULT));
        browse.setOnMouseReleased(event -> mainScene.setCursor(Cursor.DEFAULT));
        browse.relocate(230, 40);

        root.getChildren().addAll(lbl_rtl, or, browse, lbl_rtl_subtitle);

        TextArea rtl_code = new TextArea();
        rtl_code.setPrefWidth(400);
        rtl_code.setPrefHeight(600);
        rtl_code.setLayoutX(50);
        rtl_code.setLayoutY(135);
        rtl_code.setStyle("-fx-control-inner-background:rgba(0,0,0,0.50); -fx-highlight-text-fill:rgba(0,0,0,0.50);");
        root.getChildren().add(rtl_code);
        browse.setOnMouseClicked(event -> {
            mainScene.setCursor(Cursor.HAND);
            my_tmp_file[0] = fileChooser.showOpenDialog(stage);
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(my_tmp_file[0]));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            String st;
            try {
                while ((st = br.readLine()) != null)
                    rtl_code.setText(rtl_code.getText() + st + "\n");

            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        Label signiture = new Label("By Shabnam Sheikhha");
        signiture.setFont(Font.font("Serif", FontWeight.NORMAL, 15));
        signiture.setStyle("-fx-color: gray");
        signiture.relocate(50, 750);

        Label email = new Label("sheikhha@ce.sharif.edu");
        email.setFont(Font.font("Serif", FontWeight.NORMAL, 15));
        email.setStyle("-fx-color: gray");
        email.relocate(50, 765);
        root.getChildren().addAll(signiture, email);

        MyButton next = new MyButton(new Image(getClass()
                .getResource( "res/next.png").toExternalForm()));
        next.setOnMouseClicked(event -> {
            Verilog = "";
            RTL.clear();
            Collections.addAll(RTL, rtl_code.getText().split("\n"));

            assign();
            inputs.relocate(500, 35);
            if (!root.getChildren().contains(inputs))
                root.getChildren().add(inputs);
        });
        next.relocate(400, 60);
        root.getChildren().add(next);


        lbl_verilog = new MyText("Verilog:", 25, 980, 50);
        lbl_verilog.setVisible(false);
        root.getChildren().add(lbl_verilog);

        verilog_code = new TextArea();
        verilog_code.setPrefWidth(400);
        verilog_code.setPrefHeight(600);
        verilog_code.setLayoutX(980);
        verilog_code.setLayoutY(125);
        verilog_code.setStyle("-fx-control-inner-background:rgba(0,0,0,0.50); -fx-highlight-text-fill:rgba(0,0,0,0.50);");
        verilog_code.setVisible(false);
        root.getChildren().add(verilog_code);

        save_lbl = new MyText("Save file... ", 20, 980, 90);
        save_lbl.setVisible(false);

        FileChooser fileSaver = new FileChooser();
        save = new MyButton(new Image(getClass()
                .getResource( "res/save.png").toExternalForm()), 40, 40);
        save.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                File file = fileSaver.showSaveDialog(stage);
                if(file != null){
                    SaveFile(Verilog, file);
                }
            }
        });
        save.relocate(1075, 70);
        save.setVisible(false);
        root.getChildren().addAll(save, save_lbl);

        return root;
    }

    private void SaveFile(String content, File file){
        try {
            FileWriter fileWriter = null;

            fileWriter = new FileWriter(file);
            fileWriter.write(content);
            fileWriter.close();
        } catch (IOException ex) {

        }

    }
}


class MenuItem extends StackPane{
    private MenuItemBorder around = new MenuItemBorder();
    private Text text;

    public MenuItem(String label){
        setAlignment(Pos.CENTER);

        text = new Text(label);
        text.setFont(Font.font("Serif", FontWeight.NORMAL, 30));

        getChildren().addAll(around, text);
        setActive(false);

        setOnMouseEntered(event -> setActive(true));
        setOnMouseExited(event -> setActive(false));
    }

    public void setActive(boolean state){
        Simulator.menuScene.setCursor(state ? Cursor.HAND : Cursor.DEFAULT);
        around.setVisible(state);
        text.setFill(state ? Color.WHITESMOKE : Color.GRAY);
    }
}

class MyButton extends StackPane{
    private javafx.scene.image.ImageView picture;

    public MyButton(Image view){
        setAlignment(Pos.CENTER);
        this.picture = new javafx.scene.image.ImageView(view);
        this.picture.setFitWidth(50);
        this.picture.setFitHeight(50);
        setWidth(50);
        setHeight(50);
        getChildren().add(picture);
        setOnMouseEntered(event -> Simulator.mainScene.setCursor(Cursor.HAND));
        setOnMouseExited(event -> Simulator.mainScene.setCursor(Cursor.DEFAULT));
    }

    public MyButton(Image view, int w, int h){
        setAlignment(Pos.CENTER);
        this.picture = new javafx.scene.image.ImageView(view);
        this.picture.setFitWidth(50);
        this.picture.setFitHeight(50);
        setWidth(w);
        setHeight(h);
        getChildren().add(picture);
    }


}

class MenuItemBorder extends Rectangle{

    public MenuItemBorder(){
        setStrokeType(StrokeType.INSIDE);
        setStroke(Color.rgb(38, 139, 176));
        setHeight(60);
        setWidth(125);
    }
}

class MyText extends Text{
    public MyText(String text, int x, int y) {
        super(text);
        setFont(Font.font("Serif", FontWeight.NORMAL, 25));
        relocate(x, y);
        setFill(Color.WHITESMOKE);
    }

    public MyText(String text, int size, int x, int y) {
        super(text);
        setFont(Font.font("Serif", FontWeight.NORMAL, size));
        relocate(x, y);
        setFill(Color.WHITESMOKE);
    }

    public MyText(String text, int size) {
        super(text);
        setFont(Font.font("Serif", FontWeight.NORMAL, size));
        setFill(Color.WHITESMOKE);
    }
}