//output
var shadowOutput = {
  stdout: {
    pluginClass : "com.maxleap.shadow.impl.plugins.output.ShadowOutputStdout",
    config : {}
  }
};


//input
var shadowInput = {
  dir : {
    pluginClass:"com.maxleap.shadow.impl.plugins.input.dir.ShadowInputDir",
    output : "stdout",
    decodec : "com.maxleap.shadow.impl.codec.LineFeed",

    config : {
      paths : [{
        startPath:"./logs",
        pattern:".*-json.log", //this could be regex express
        match:function(fileFullPath, logContent) {
          var jsonLog = JSON.parse(logContent);
          jsonLog.filePath = fileFullPath;
          return jsonLog;
        }
      }
							]
    }
  }



};
