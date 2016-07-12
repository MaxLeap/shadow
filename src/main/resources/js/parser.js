var parser = {};

parser.dirMatch = function(dirPath, line) {
  var strArr = line.split(" "),
      result = "";
  for (i = 0; i < strArr.length; i++) {
    result += strArr[i] + "|";
  }
  return {
    path:dirPath,
    log:result
  };
};

