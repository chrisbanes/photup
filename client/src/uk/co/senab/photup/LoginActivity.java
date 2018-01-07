/*
 * Copyright 2013 Chris Banes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.senab.photup;
                                               
import com.facebook.android.AsyncFacebookRunner;                      //一系列的facebook监听器
import com.facebook.android.AsyncFacebookRunner.RequestListener;    
import com.facebook.android.AsyncFacebookRunner.SimpleRequestListener;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;

import org.json.JSONException;              //json= JavaScript 对象表示法(JavaScript Object Notation) 
import org.json.JSONObject;

import android.app.Activity;                //应用了一些android自带组件
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import java.io.FileNotFoundException;       //java输入输出流相关包
import java.io.IOException;
import java.net.MalformedURLException;

import uk.co.senab.photup.facebook.Session;  //作者文件的其他包（有关facebook的会话）   

public class LoginActivity extends Activity implements View.OnClickListener, DialogListener { 
    //调用接口：点击和会话的监听器，

    static final int REQUEST_FACEBOOK_SSO = 100;

    
    private Facebook mFacebook;    //facebook对象
    private View mAboutLogo;       //头像视图
    private Button mLoginBtn, mLogoutBtn, mLibrariesBtn;   //登录按钮，登出按钮，libraies图书馆按钮？？
    private View mFacebookBtn, mTwitterBtn;   //facebook和twitter的按钮的视图，表示以何种方式登陆
    private TextView mMessageTv;   //页面提示的文本视图（如果用户没登陆就让它登陆，如果用户登陆了就欢迎）
    private CheckBox mLoginPromoCheckbox;    //免登陆的勾选框

    public void onClick(View v) {         //当点击到某些视图会发生的事              
        if (v == mLoginBtn) {             //点击到登陆按钮时
            loginToFacebook();            //调用loginToFacebook（）登录到facebook
        } else if (v == mLogoutBtn) {     //点击到登出按钮时
            showLogoutPrompt();           //显示登出的确认窗口
        } else if (v == mFacebookBtn) {   //如果选择了以facebook登陆则
            startActivity(new Intent(Intent.ACTION_VIEW,  //通过intent得到当前facebook的地址
                    Uri.parse(getString(R.string.facebook_address))));
        } else if (v == mTwitterBtn) {    //如果选择了twitter，同上
            startActivity(
                    new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.twitter_address))));
        } else if (v == mLibrariesBtn) {  //如果选择了图书馆？？？按钮，则连接到类LicencesActivity
            startActivity(new Intent(this, LicencesActivity.class));
        } else if (v == mAboutLogo) {     
            onBackPressed();             //返回
        }
    }

    @Override //看看这个人有没有facebook账号，如果没有，可以新建一个
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (null == mFacebook) {
            mFacebook = new Facebook(Constants.FACEBOOK_APP_ID);
            mFacebook.setAuthorizeParams(this, REQUEST_FACEBOOK_SSO);
        }
        mFacebook.authorizeCallback(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override  
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_top); //返回时的页面切换效果，
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {    //调用一堆res/layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);            //显示login的页面

        mAboutLogo = findViewById(R.id.ll_about_logo);      //找到头像的位置

        mLoginBtn = (Button) findViewById(R.id.btn_login);  //找到登陆的按钮
        mLoginBtn.setOnClickListener(this);

        mLogoutBtn = (Button) findViewById(R.id.btn_logout);//找到登出的按钮
        mLogoutBtn.setOnClickListener(this);

        mFacebookBtn = findViewById(R.id.tv_social_fb);     //找到用facebook登陆的按钮
        mFacebookBtn.setOnClickListener(this);

        mTwitterBtn = findViewById(R.id.tv_social_twitter); //找到用twitter登陆的按钮
        mTwitterBtn.setOnClickListener(this); 

        mLibrariesBtn = (Button) findViewById(R.id.btn_libraries);  //找到用libraries的按钮
        mLibrariesBtn.setOnClickListener(this);

        mLoginPromoCheckbox = (CheckBox) findViewById(R.id.cbox_login_promo);  //找到免登陆按钮

        mMessageTv = (TextView) findViewById(R.id.tv_login_message);  //找到请登陆提示的显示文本视图

        final String action = getIntent().getAction();
        if (Constants.INTENT_NEW_PERMISSIONS.equals(action)) {   //应该和import的类constants有关 结果是判断登陆还是登出facebook
            loginToFacebook();   
        } else if (Constants.INTENT_LOGOUT.equals(action)) {     
            logoutOfFacebook();
        }
    }

    @Override
    protected void onStart() {  
        super.onStart();
        refreshUi();   //刷新
    }

    private void loginToFacebook() {  //登陆到facebook的方法
        mFacebook = new Facebook(Constants.FACEBOOK_APP_ID);
        mFacebook.authorize(this, Constants.FACEBOOK_PERMISSIONS,  //这边的authorize（授权）应该是import的Facebook类中的一个方法
                BuildConfig.DEBUG ? Facebook.FORCE_DIALOG_AUTH    //a?b:c 如果c！=0，a;c==0,b
                        : REQUEST_FACEBOOK_SSO, this);
    }

    private void logoutOfFacebook() {  //登出facebook的方法
        // Actual log out request （真登出：应该是指清理了缓存之类的那种）
        Session session = Session.restore(this);
        if (null != session) {
            new AsyncFacebookRunner(session.getFb()).logout(getApplicationContext(), 
                    new AsyncFacebookRunner.SimpleRequestListener());
        }

        Session.clearSavedSession(this);  
        PhotoUploadController.getFromContext(this).reset();   //图片重新加载

        refreshUi(); //重新加载页面
    }

    private void refreshUi() {     //重新加载页面的方法会干什么
        Session session = Session.restore(this);           //恢复到当前的会话
        if (null != session) {     
            mMessageTv.setVisibility(View.GONE);           //登陆提示，登陆按钮小时，免登陆勾选框消失
            mLoginBtn.setVisibility(View.GONE);
            mLoginPromoCheckbox.setVisibility(View.GONE);
            mLogoutBtn.setText(getString(R.string.logout, session.getName()));  //登出按钮上的字设为用户名
            mLogoutBtn.setVisibility(View.VISIBLE);        //登出的按钮显示
            mLibrariesBtn.setVisibility(View.VISIBLE);     //libraries按钮显示
            mAboutLogo.setOnClickListener(this);           //给头像设一个点击的监听器，传入值为此次登陆
        } else {
            mMessageTv.setText(R.string.welcome_message);   //把提示信息设为欢迎
            mMessageTv.setVisibility(View.VISIBLE);         //提示信息可见
            mLoginBtn.setVisibility(View.VISIBLE);          //登陆按钮可见
            mLoginPromoCheckbox.setVisibility(View.VISIBLE);//免登陆勾选框可见
            mLogoutBtn.setVisibility(View.GONE);            //登出按钮不可见
            mLibrariesBtn.setVisibility(View.GONE);         //libraries的按钮不可见
            mAboutLogo.setOnClickListener(null);            //给头像设一个监听器，传入值为0
            /*
            对比之前的，可以看出要在头像的这个地方设置监听器的原因，
            应该是，如果用户是第一次登陆，比如用别人手机登陆，登陆时头像的部分会是系统的内置头像
            而如果这是你自己的手机，曾经多次登陆过，只是上一次也登出了，那就会显示你的头像
            */
        }
    }

    private void saveFacebookSession() {  //保存facebook的会话
        AsyncFacebookRunner fbRunner = new AsyncFacebookRunner(mFacebook);
        fbRunner.request("me", new RequestListener() {

            public void onComplete(String response, Object state) {
                try {
                    JSONObject object = new JSONObject(response);
                    String id = object.getString("id");
                    String name = object.getString("name");

                    Session session = new Session(mFacebook, id, name);
                    session.save(getApplicationContext());

                    setResult(RESULT_OK);
                    finish();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            public void onFacebookError(FacebookError e, Object state) {
                e.printStackTrace();
            }

            public void onFileNotFoundException(FileNotFoundException e, Object state) {
                e.printStackTrace();
            }

            public void onIOException(IOException e, Object state) {
                e.printStackTrace();
            }

            public void onMalformedURLException(MalformedURLException e, Object state) {
                e.printStackTrace();
            }
        });
    }

    private void postPromoPost() {  //发进一步的报告（发给程序内部别的地方），比如免登陆的勾了之类的
        Bundle b = new Bundle();    
        b.putString("message", getString(R.string.promo_text)); 
        b.putString("link", Constants.PROMO_POST_URL);
        b.putString("picture", Constants.PROMO_IMAGE_URL);

        AsyncFacebookRunner fbRunner = new AsyncFacebookRunner(mFacebook);
        fbRunner.request("me/feed", b, "POST", new SimpleRequestListener(), null);  
    }

    private void showLogoutPrompt() {  //登出的确认窗口的方法

        AlertDialog.Builder builder = new AlertDialog.Builder(this); //定义了一个builder对像建立警告会话窗
        builder.setTitle(R.string.logout_prompt_title);
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) { //登出facebook 会话解除
                logoutOfFacebook();    
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);  //builder 建立导航按钮

        builder.show(); 
    }

    public void onCancel() {  
    }

    public void onComplete(Bundle values) {       //把values传入
        if (mLoginPromoCheckbox.isChecked()) {    
            postPromoPost();
        }
        saveFacebookSession();
    }
    
    //后面两个都是异常处理，处理方式为直接打出信息
    public void onError(DialogError e) { 
        e.printStackTrace();
    }

    public void onFacebookError(FacebookError e) {
        e.printStackTrace();
    }

}
