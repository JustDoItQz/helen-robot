package com.helen.robot.controller;

import com.helen.robot.api.WechatTools;
import com.helen.robot.core.Core;
import com.helen.robot.service.ILoginService;
import com.helen.robot.service.impl.LoginServiceImpl;
import com.helen.robot.thread.CheckLoginStatusThread;
import com.helen.robot.utils.SleepUtils;
import com.helen.robot.utils.tools.CommonTools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 登陆控制器
 *
 */
public class LoginController {
    private static Logger LOG = LoggerFactory.getLogger(LoginController.class);
    private ILoginService loginService = new LoginServiceImpl();

    public void login(String qrPath) {
        if (Core.getInstance().isAlive()) { // 已登陆
            LOG.info("itchat4j已登陆");
            return;
        }

        for (int count = 0; count < 10; count++) {
            LOG.info("获取UUID");
            while (loginService.getUuid() == null) {
                LOG.info("1. 获取微信UUID");
                while (loginService.getUuid() == null) {
                    LOG.warn("1.1. 获取微信UUID失败，两秒后重新获取");
                    SleepUtils.sleep(2000);
                }
            }
            LOG.info("2. 获取登陆二维码图片");
            if (loginService.getQR(qrPath)) {
                break;
            } else if (count == 10) {
                LOG.error("2.2. 获取登陆二维码图片失败，系统退出");
                System.exit(0);
            }
        }
        LOG.info("3. 请打开浏览器输入http://127.0.0.1:8088,扫描二维码图片，并在手机上确认");
        if (!Core.getInstance().isAlive()) {

            // 一直等待，直到手机确认成功，大概125s超时
            if (loginService.waitForLogin()) {
                Core.getInstance().setAlive(true);
                LOG.info(("4. 登陆成功"));
            } else {
                LOG.info("4. 登陆超时，直接退出当前线程，需要重置，http://127.0.0.1:8088/helen");
                return;
            }
        }

        LOG.info("5. 登陆成功，微信初始化");
        if (!loginService.webWxInit()) {
            LOG.info("6. 微信初始化异常");
            System.exit(0);
        }

        LOG.info("6. 开启微信状态通知");
        loginService.wxStatusNotify();

        LOG.info("7. 清除。。。。");
        CommonTools.clearScreen();
        LOG.info(String.format("欢迎回来， %s", Core.getInstance().getNickName()));

        LOG.info("8. 开始接收消息");
        loginService.startReceiving();

        LOG.info("9. 获取联系人信息");
        loginService.webWxGetContact();

        LOG.info("10. 获取群好友及群好友列表");
        loginService.WebWxBatchGetContact();

        LOG.info("11. 缓存本次登陆好友相关消息");
        WechatTools.setUserInfo(); // 登陆成功后缓存本次登陆好友相关消息（NickName, UserName）

        LOG.info("12.开启微信状态检测线程");
        new Thread(new CheckLoginStatusThread()).start();
    }
}