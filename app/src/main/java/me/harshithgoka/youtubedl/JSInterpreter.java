package me.harshithgoka.youtubedl;

import android.util.Log;
import android.util.Pair;

import org.json.JSONObject;
import org.mozilla.javascript.FunctionObject;
import org.mozilla.javascript.Parser;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.harshithgoka.youtubedl.Utils.Arg;
import me.harshithgoka.youtubedl.Utils.Fun;

/**
 * Created by harshithg on 16/1/18.
 */

public class JSInterpreter {
    String code;
    HashMap<String, Fun> functions;

    String _NAME_RE = "[a-zA-Z_$][a-zA-Z_$0-9]*";

    JSInterpreter(String code) {
        this.code = code;
    }

    public Fun extractFunction(String funcname) {

//        String funcname = "NJ";
        String s = String.format("(?x)" +
                "(?:function\\s+%s|[{;,]\\s*%s\\s*=\\s*function|var\\s+%s\\s*=\\s*function)\\s* " +
                "\\((?<args>[^)]*)\\)\\s*" +
                "\\{(?<code>[^}]+)\\}", Pattern.quote(funcname), Pattern.quote(funcname), Pattern.quote(funcname));
        Pattern funcPattern = Pattern.compile(s);
        Matcher m = funcPattern.matcher(code);
        m.find();
        String[] _args = m.group(1).split(",");

        String _code = m.group(2);

        return new Fun(funcname, _args, _code);
    }
    
    

    public Arg callFunction(Fun func, Arg[] args) throws Exception {
        if (args.length != func.argnames.length) {
            throw new Exception("Incorrect number of arguments passed");
        }

        HashMap<String, Arg> local_vars = new HashMap<>();
        for (int i = 0; i != args.length; i++) {
            local_vars.put(func.argnames[i], args[i]);
        }

        Arg res = new Arg("");
        String[] split = func.code.split(";");
        for (String stmt : split) {
            Pair<Arg, Boolean> ret = interpretStatement(stmt, local_vars, 100);
            res = ret.first;
            if (ret.second) {
                break;
            }
        }

        return res;
    }

    public Pair<Arg,Boolean> interpretStatement(String stmt, HashMap<String, Arg> local_vars, int allowRecursion) throws Exception {
        Log.d("JSParser", stmt);


        if (allowRecursion < 0) {
            throw new Exception("Recursion limit reached");
        }

        boolean should_abort = false;

        stmt = stmt.trim();
        Pattern assgn = Pattern.compile("var\\s");
        Matcher m = assgn.matcher(stmt);
        String expr = "";
        if (m.find()) {
            expr = stmt.substring(m.group(0).length());
        }
        else {
            m = Pattern.compile("return(?:\\s+|$)").matcher(stmt);
            if (m.find()) {
                expr = stmt.substring(m.group(0).length());
                should_abort = true;
            }
            else {
                expr = stmt;
            }
        }

        Arg arg = interpretExpression(expr, local_vars, allowRecursion);

        return new Pair(arg, should_abort);
    }

    private Arg interpretExpression(String expr, HashMap<String, Arg> local_vars, int allowRecursion) throws Exception {
        expr = expr.trim();

        if (expr.equals("")) {
            return new Arg();
        }

        if (expr.startsWith("(")) {
            int parens_count = 0;
            Pattern p_pr = Pattern.compile("[()]");
            Matcher m = p_pr.matcher(expr);
            while (m.find()) {
                if (m.group(0).equals("(")) {
                    parens_count += 1;
                }
                else {
                    parens_count -= 1;
                    if (parens_count == 0) {
                        String sub_expr = expr.substring(1, m.start());
                        Arg sub_result = interpretExpression(sub_expr, local_vars, allowRecursion);
                        String remaining_expr = expr.substring(m.end()).trim();
                        if (remaining_expr.equals("")) {
                            return sub_result;
                        }
                        else {
                            // TODO Replace all Arg usage with a JSONObject! xD
                            expr = Arg.class.getCanonicalName() + remaining_expr;
                        }
                    }
                }
            }
            if (parens_count > 0) {
                throw new Exception("Premature end of parens in " + expr);
            }


        }

        return new Arg("");
    }


}
