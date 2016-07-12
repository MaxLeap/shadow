//load your parser code to parser log.
load('js/parser.js');

//output
var shadowOutput = {
  stdout: {
    pluginClass : "com.maxleap.shadow.impl.plugins.output.ShadowOutputStdout",
    config : {

    }
  },

  file: {
    pluginClass : "com.maxleap.shadow.impl.plugins.output.ShadowOutputFile",
    decodec : "com.maxleap.shadow.impl.codec.MapToJson",
    config : {
      path:"/tmp/shadow.log"
    }
  },

//  http: {
//    pluginClass : "com.maxleap.shadow.impl.plugins.output.ShadowOutputHttp",
//    config : {
//      host:"127.0.0.1",
//      port:8082,
//      url:"/test/logs"
//    }
//  },
//
//  forward: {
//    pluginClass : "com.maxleap.shadow.impl.plugins.output.ShadowOutputForward",
//    config : {
//      tag:"leap.myLog",
//      address:"shadow"
//    }
//  }
};


//input
var shadowInput = {
  dir : {
    pluginClass:"com.maxleap.shadow.impl.plugins.input.dir.ShadowInputDir",
    output : shadowOutput.stdout,
    decodec : "com.maxleap.shadow.impl.codec.LineFeed",
    encodec : "com.maxleap.shadow.impl.codec.MapToJson",

    config : {
      tail:false,
      paths : [{
         startPath:"./",
         pattern:".*.log", //this could be regex express
         match:function(fileFullPath, logContent) {
            //return a javascript object
            return parser.dirMatch(fileFullPath, logContent);
          }
        }
      ]
    }
  }

  //startup a http server for receive logs
//  http : {
//    pluginClass:"com.maxleap.shadow.impl.plugins.input.http.ShadowInputHttp",
//    output:shadowOutput.stdout,
//    decodec : "com.maxleap.shadow.impl.codec.StringToJson",
//    encodec : "com.maxleap.shadow.impl.codec.MapToJson",
//
//    config : {
//      host:"192.168.99.101",
//      port:8088,
//      matchAll:false,
//      paths:[
//        {
//          path:"/test/*",
//          match:function(requestPath, logContentJson) {
//            return {
//              requestPath:requestPath,
//              logContent:logContentJson.message
//            };
//          }
//        }
//      ]
//    }
//  },

//  dockerStats: {
//      coordinate:"com.maxleap:shadow-docker-stats:0.1.0-SNAPSHOT",
//      pluginClass:"com.maxleap.shadow.plugin.input.docker.ShadowInputDockerStats",
//      output:shadowOutput.stdout,
//      encodec : "com.maxleap.shadow.impl.codec.MapToJson",
//
//      config : {
//        groups : [
//          {
//            //all fn have to return true then invoke function of match with config
//            predicate:[
//              {
//                fnOne:function(config) {
//                  return true;
//                }
//              },
//              {
//                fnTwo:function(config) {
//                  return true;
//                }
//              }
//            ],
//
//            //return a object with your custom
//            match:function(config, stats) {
//              return {
//                containerId:config.id,
//                containerName:config.name,
//                stats:stats
//              };
//            }
//          }
//        ]
//      }
//    }

};
