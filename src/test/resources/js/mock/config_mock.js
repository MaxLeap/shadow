var shadowConfigMock = {
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
}