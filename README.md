### Shadow
The target is replace logstash

### Deploy
docker run -d -v /opt/tools/shadow/js:/opt/tools/shadow/js maxleap/shadow:0.1.1  

这里需要把项目的js文件夹映射出来，因为shadow的配置就是js文件，具体可以参考相关的[例子项目](https://gitlab.maxleap.com/maxleapdevops/shadow-js-config/blob/master/example-shadow.js)  
这里面 **shadowInput** 就是具体的插件,里面相关的配置。

### js function
TODO
