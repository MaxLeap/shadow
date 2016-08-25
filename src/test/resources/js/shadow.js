//output
var shadowOutput = {
  stdout: {
    pluginClass : "com.maxleap.shadow.impl.plugins.output.ShadowOutputStdout",
    config : {

    }
  },

  file: {
    pluginClass : "com.maxleap.shadow.impl.plugins.output.ShadowOutputFile",
    decodec : "com.maxleap.shadow.impl.codec.JsonToString",
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
      defaultURI:"/test/logs",
      hosts:["localhost:8081", "localhost:8081"]
    }
  }
};


//input
var shadowInput = {
  dir : {
    pluginClass:"com.maxleap.shadow.impl.plugins.input.dir.ShadowInputDir",
    shadowOutputName : "file",
    decodec : "com.maxleap.shadow.impl.codec.LineFeed",
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
    shadowOutputName : "http",
    decodec : "com.maxleap.shadow.impl.codec.BufferToJson",
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
    shadowOutputName : "forward",
    //decodec : "com.maxleap.shadow.impl.codec.JsonToMap",
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
