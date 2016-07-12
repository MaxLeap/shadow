//output
var shadowOutput = {
  stdout: {
    pluginClass : "com.maxleap.shadow.impl.plugins.output.ShadowOutputStdout"
  }
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



};
