
```
                      _   _  __  __         ______
  _ __ ___  __ _  ___| |_(_)/ _|/ _|       / / / /
 | '__/ _ \/ _` |/ __| __| | |_| |_ _____ / / / /
 | | |  __/ (_| | (__| |_| |  _|  _|_____/ / / /
 |_|  \___|\__,_|\___|\__|_|_| |_|      /_/_/_/

```

# What is it ?

`reactiff` is a simple glue between your micro servive and the `reactor-netty` library.

The aims is to bring full reactive HTTP communication in a non intrusive library to unleash `reactor-netty` power!

# What does it contain ?

- A simple DSL based on annotations to declare HTTP handlers and filters.
- A reactive implementation of codecs : text, files, json, binary (smile).
- The `Codec` interface to extend coding and decoding of client and server request's content. The codec choice is based on a HTTP compliant content negociation.

# What are the main lead

- be a simple zero waste API
- only a one goal library; not a framework. We did not make any choice : no dependency injection required, no templates mechanism provided...
- java and OSGI support
- easily testable

# Example

A simple controller implementing `ReactiveHandler` :

```java
public final class MyHandler implements ReactiveHandler {
    
    @RequestMapping(method = GET, path = "/pojo")
    public Flux<Pojo> list() {
        Flux<Pojo> reactiveList = null; // non blocking database call
        return reactiveList;
    }
    
}
```

running in a simple java application :

```java
public final class Application {
    
    public static void main(String[] args){
        CodecManager codecManager = new CodecManagerImpl();
        codecManager.addCodec(new JsonCodec(new ObjectMapper()));
        
        ReactiveHttpServer server = ReactiveHttpServer.create()
                        .codecManager(codecManager)
                        .addHandler()
                        .build();
        
        server.addReactiveFilter(DefaultFilters.cors());
        server.addReactiveHandler(new MyHandler());
        
        server.start();
    }
    
}
```

# Packages description

- `reactiff-api` : the java API which contains codec and server public API.
- `reactiff-codec` : basic codecs : text, file and binary.
- `reactiff-codec-jackson` : json and smile codecs implemented with jackson.
- `reactiff-server` : the server implementation (wrapping handlers and filters with reactor-netty)
