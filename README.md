# GoogleAuthentication
Google Authentication for unity
### 先到Google Cloud 创建两个凭证
![img.png](img.png)
### 一个安卓的凭证, 需要填入包名, Android秘钥指纹
![img_1.png](img_1.png)
### 第二个Web应用
![img_2.png](img_2.png)
### 复制Web应用的Client Id后续会用到
![img_3.png](img_3.png)
## 编译unity安卓插件
添加依赖
```
    implementation("androidx.credentials:credentials:1.3.0")
    implementation('com.google.android.libraries.identity.googleid:googleid:1.1.1')
```
详细请参考 https://developer.android.com/identity/sign-in/credential-manager
实现代码参考 仓库中 GoogleAuthentication.java
## 登录有两种弹窗 
### 一种通过
```java
new GetGoogleIdOption.Builder()
        .setServerClientId(clientId)
```

### 另一种通过
```java
new GetSignInWithGoogleOption.Builder(clientId)
```
这里的clientId就是从Google Cloud复制的Web应用的Client Id

### 编译安卓插件后放入Unity项目 Assets/Plugins/Android 目录下
这里还需要自定义 mainTemplate.gradle
![img_4.png](img_4.png)
在mainTemplate.gradle中添加编译依赖
![img_5.png](img_5.png)
### C#中使用方法
这里使用AndroidJavaProxy的方式获得Java中的回调
在C#从创建SignInRequest : AndroidJavaProxy
```Csharp
    class SignInRequest : AndroidJavaProxy
    {
        public SignInRequest() : base("com.oali.game.GoogleAuthentication$SignInRequest")
        {
        }

        string getServerClientId()
        {
            return "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.apps.googleusercontent.com";
        }

        void onReply(string userDataJson)
        {
            Debug.Log($"SignInRequest.onReply {userDataJson}");
            var userData = JsonUtility.FromJson<MyClass>(userDataJson);
            var postData = new GoogleSignInRequest();
            postData.IdToken = userData.token;
            var postJsonData = JsonUtility.ToJson(postData);
            var unityWebRequest = UnityWebRequest.Post("http://192.168.0.200:5092/sign-in/google", postJsonData, "application/json");
            var operation = unityWebRequest.SendWebRequest();
            operation.completed += asyncOperation =>
            {
                var req = (UnityWebRequestAsyncOperation)asyncOperation;
                var text = req.webRequest.downloadHandler.text;
                Debug.Log(text);
            };
        }

        void onError(string exceptionMessage)
        {
            Debug.LogError($"SignInRequest.onError {exceptionMessage}");
        }
    }
```
这里基类中的参数
```
base("com.oali.game.GoogleAuthentication$SignInRequest")
```
### com.oali.game是package name
### GoogleAuthentication是类名
### $SignInRequest 是嵌套的interface名
调用通过Java的静态方法, 传入C#的对象即可实现通信
```
using (var androidJavaClass = new AndroidJavaClass("com.oali.game.AppSignin"))
{
    androidJavaClass.CallStatic("GoogleSignIn", new SignInRequest());
}
```