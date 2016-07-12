//output
var shadowOutput = {
  stdout: {
    pluginClass : "com.maxleap.shadow.impl.plugins.output.ShadowOutputStdout",
    config : {

    }
  },

  file: {
    pluginClass : "com.maxleap.shadow.impl.plugins.output.ShadowOutputFile",
    config : {
      path:"/tmp/shadow.log"
    }
  },

  forward: {
    pluginClass : "com.maxleap.shadow.impl.plugins.output.ShadowOutputForward",
    config : {
      tag:"leap.myLog",
      address:"targetShadow"
    }
  },

  http: {
    pluginClass : "com.maxleap.shadow.impl.plugins.output.ShadowOutputHttp",
    config : {
      host:"localhost",
      port:8081,
      uri:"/test/logs"
    }
  }
};


//input
var shadowInput = {
  dir : {
    pluginClass:"com.maxleap.shadow.impl.plugins.input.dir.ShadowInputDir",
    output : shadowOutput.file,
    decodec : "com.maxleap.shadow.impl.codec.LineFeed",
    encodec : "com.maxleap.shadow.impl.codec.MapToJson",

    config : {
      tail:false,
      paths : [
        {
          startPath:"./",
          pattern:".*my.log",
          match:function(fileFullPath, logContent) {
            //return a javascript object
            return {
              path:fileFullPath,
              log:logContent
            };
          }
        }
      ]
    }
  },

  //startup a http server for receive logs
  http : {
    pluginClass:"com.maxleap.shadow.impl.plugins.input.http.ShadowInputHttp",
    output:shadowOutput.http,
    decodec : "com.maxleap.shadow.impl.codec.StringToMap",
    encodec : "com.maxleap.shadow.impl.codec.MapToJson",

    config : {
      host:"127.0.0.1",
      port:8082,
      //Does request would match all the paths which defined below.
      matchAll:true,
      paths:[
        {
          path:"/log1",
          match:function(requestPath, logContentJson) {
            return {
              requestPath:requestPath,
              logContent:logContentJson.message
            };
          }
        },
        {
          path:"/log2",
          match:function(requestPath, logContentJson) {
            return {
              requestPath:requestPath,
              logContent:logContentJson.message
            };
          }
        }
      ]
    }
  },

  forward:{
    pluginClass:"com.maxleap.shadow.impl.plugins.input.forward.ShadowInputForward",
    output:shadowOutput.forward,
    decodec : "com.maxleap.shadow.impl.codec.JsonToMap",
    encodec : "com.maxleap.shadow.impl.codec.MapToJson",
    config : {
      address:"anotherShadow",
      matchAll:true,
      tags:[
        {
          tag:"leap.*",
          match:function(tag, logContent) {
            console.log(tag);
            return logContent;
          }
        }
      ]
    }
  }

//  dockerStats: {
//    coordinate:"com.maxleap:shadow-docker-stats:0.1.0-SNAPSHOT",
//    pluginClass:"com.maxleap.shadow.plugin.input.docker.ShadowInputDockerStats",
//    output:shadowOutput.stdout,
//    encodec : "com.maxleap.shadow.impl.codec.MapToJson",
//
//    config : {
//      groups : [
//        {
//          //all fn have to return true then invoke function of match with config
//          predicate:[
//            {
//              fnOne:function(config) {
//                return true;
//              }
//            },
//            {
//              fnTwo:function(config) {
//                return true;
//              }
//            }
//          ],
//
//          //return a object with your custom
//          match:function(config, stats) {
//            return {
//              containerId:config.id,
//              containerName:config.name,
//              stats:stats
//            };
//          }
//        }
//      ]
//    }
//  }
};
