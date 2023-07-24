package com.wdf.fudoc.request.js;

import com.wdf.fudoc.common.constant.FuConsoleConstants;
import com.wdf.fudoc.components.FuConsole;
import com.wdf.fudoc.console.FuLogger;
import com.wdf.fudoc.request.js.context.FuContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

/**
 * 执行JS脚本
 *
 * @author wangdingfu
 * @date 2023-05-31 10:18:41
 */
@Slf4j
public class JsExecutor {


    /**
     * 执行javaScript脚本
     *
     * @param fuContext 脚本中的上下文对象
     */
    public static void execute(FuContext fuContext, FuLogger fuLogger) {
        String script = fuContext.getScript();
        if (StringUtils.isBlank(script)) {
            return;
        }
        Context cx = Context.enter();
        long start = System.currentTimeMillis();
        boolean success = false;
        try {
            // 将Java对象绑定到Rhino执行上下文中
            ScriptableObject scriptableObject = cx.initStandardObjects();
            ScriptableObject.putProperty(scriptableObject, "fu", fuContext);
            fuLogger.setPrefix(fuContext.getScriptName());
            ScriptableObject.putProperty(scriptableObject, "console", fuLogger);
            fuLogger.info(FuConsoleConstants.START);
            cx.evaluateString(scriptableObject, fuContext.getScript(), "<cmd>", 1, null);
            success = true;
        } catch (Exception e) {
            log.info("执行脚本【{}】异常", fuContext.getScriptName(), e);
            fuLogger.info(e.toString());
        } finally {
            logResult(fuLogger, System.currentTimeMillis() - start, success);
            fuLogger.setPrefix(null);
            Context.exit();
        }
    }

    private static void logResult(FuLogger fuLogger, Long time, boolean success) {
        fuLogger.info(FuConsoleConstants.LINE);
        fuLogger.info("EXECUTE " + (success ? "SUCCESS" : "FAIL"));
        fuLogger.info("Total time: " + time + " ms");
        fuLogger.info(FuConsoleConstants.END + "\n");
    }
}
