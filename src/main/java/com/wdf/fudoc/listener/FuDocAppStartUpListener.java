package com.wdf.fudoc.listener;


import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.wdf.fudoc.FuDocNotification;
import com.wdf.fudoc.config.state.FuDocSecuritySetting;
import com.wdf.fudoc.pojo.dto.CommonResult;
import com.wdf.fudoc.pojo.dto.VersionInfoDTO;
import com.wdf.fudoc.pojo.dto.VersionInfoVO;
import com.wdf.fudoc.view.FuDocGeneralForm;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;


/**
 * @author wangdingfu
 * @date 2022-08-14 22:32:40
 */
public class FuDocAppStartUpListener implements StartupActivity, DumbAware {
    private static final Logger LOGGER = Logger.getInstance(FuDocGeneralForm.class);

    private static final String PRIMARY_KEY = "dfe68b77d54943fc8d481c6ae80a2a9d";

//    private static final String URL = "http://150.158.164.160:9090/fu_doc/version_info";
    private static final String URL = "http://localhost:9090/fu_doc/version_info";

    private static final int DAYS = 60 * 60 * 24 * 7;
    private static final int ONE_DAY = 60 * 60 * 24;

    @Override
    public void runActivity(@NotNull Project project) {
        LOGGER.info("应用已经启动了");
        FuDocSecuritySetting instance = FuDocSecuritySetting.getInstance(project);
        String uniqId = instance.getUniqId();
        long time = instance.getTime();
        long currentSeconds = DateUtil.currentSeconds();
        if (time > currentSeconds) {
            //无需请求
            return;
        }
        VersionInfoDTO versionInfoDTO = new VersionInfoDTO();
        IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginId.getId("com.wdf.api"));
        //获取当前插件版本
        versionInfoDTO.setUniqId(uniqId);
        if (Objects.nonNull(plugin)) {
            versionInfoDTO.setPluginVersion(plugin.getVersion());
        }
        //请求参数
        byte[] request = SecureUtil.aes(PRIMARY_KEY.getBytes()).encrypt(JSONUtil.toJsonStr(versionInfoDTO));
        try {
            String result = HttpUtil.createPost(URL).body(request).timeout(3000).execute().body();
            if (StringUtils.isNotBlank(result)) {
                CommonResult<VersionInfoVO> commonResult = JSONUtil.toBean(result, new TypeReference<>() {
                }, true);
                VersionInfoVO data = commonResult.getData();
                String message = null;
                long newTime = currentSeconds + DAYS;
                if (Objects.nonNull(data) && data.getCode() == 1 && data.getTime() > 0) {
                    message = data.getMessage();
                    newTime = data.getTime();
                }
                instance.setUniqId(data.getUniqId());
                instance.setTime(newTime);
                instance.loadState(instance);
                if (StringUtils.isNotBlank(message)) {
                    FuDocNotification.notifyInfo(message);
                }
            }
            LOGGER.info(result);
        } catch (Exception e) {
            instance.setTime(currentSeconds + ONE_DAY);
            instance.loadState(instance);
            LOGGER.error("请求获取版本信息失败", e);
        }
    }
}
