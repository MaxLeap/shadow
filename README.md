### Shadow
基于Java8的支持DSL的 日志处理收集工具，用于代替logstash.

#### 核心原理
通过DSL文件 Shadow.java 来定义一系列的流程.  
- 比如定义一个Input接口，里面定义了 *matchFunction* 用来配置信息流里面的匹配方式
- *decode* 用来解析相关的数据类型
- *handler* 用来处理相关的逻辑
- 最后通过 *addOutput* 接口，将消息传出去
- 相应的Output接口里也有相关的方法
- *tokenFunction* 就是用来过滤出指定的消息
- *handler* 用来处理消息，当然如果你不想处理消息，可以不写
- *encode* 用来对消息进行最后的处理，最后Output实现者会把消息发送出去

#### Shadow支持的中间件
目前Shadow用来消费Kafka的Topic,或者直接将消息发送到ES里去，后期可以考虑加入更多的Plugin来扩展其功能


#### TODO
- ES插件还没有完善
- 流控，如果Output挂了，则需要保证停止消息的接受,或者把消息暂存到本地.
