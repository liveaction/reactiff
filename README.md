
```
                      _   _  __  __         ______
  _ __ ___  __ _  ___| |_(_)/ _|/ _|       / / / /
 | '__/ _ \/ _` |/ __| __| | |_| |_ _____ / / / /
 | | |  __/ (_| | (__| |_| |  _|  _|_____/ / / /
 |_|  \___|\__,_|\___|\__|_|_| |_|      /_/_/_/

```

# What is it ?

`reactiff` is a simple glue between your micro servives and the `reactor-netty` library.

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
public final class PojoHandler implements ReactiveHandler {

    @RequestMapping(method = GET, path = "/pojo")
    public Flux<Pojo> list() {
        return Flux.range(0, 10)
                .delayElements(Duration.ofMillis(50))
                .map(index -> new Pojo(String.valueOf(index), "Hello you"));
    }

}
```

running in a simple java application :

```java
public final class ExampleApp {

    public static void main(String[] args) {
        CodecManager codecManager = new CodecManagerImpl();
        codecManager.addCodec(new JsonCodec(new ObjectMapper()));

        ReactiveHttpServer server = ReactiveHttpServer.create()
                .protocols(HttpProtocol.HTTP11)
                .codecManager(codecManager)
                .port(3000)
                .build();

        server.addReactiveHandler(new PojoHandler());
        server.start();
    }

}
```

That can be consumed with,

- a simple `reactor-netty` client :

```java
public final class ExampleApp {

    public static void main(String[] args) {
        CodecManager codecManager = new CodecManagerImpl();
        codecManager.addCodec(new JsonCodec(new ObjectMapper()));
                
        Flux<Pojo> response = HttpClient.create()
                .get()
                .uri("http://localhost:3000/pojo")
                .response(codecManager.decodeAsFlux(Pojo.class));

        response.subscribe(System.out::println);

        System.out.println(String.format("Received %d results", response.count().block()));
    }

}
```

- or a curl :

```bash
14:48 $ http ':3000/pojo'                                                                                                                                                                                                                                                                                                    
HTTP/1.1 200 OK                                                                                                                                                                                                                                                                                                              
content-type: application/json
transfer-encoding: chunked

[
    {
        "id": "0",
        "name": "Hello you"
    },
    {
        "id": "1",
        "name": "Hello you"
    },
    {
        "id": "2",
        "name": "Hello you"
    },
    {
        "id": "3",
        "name": "Hello you"
    },
    {
        "id": "4",
        "name": "Hello you"
    },
    {
        "id": "5",
        "name": "Hello you"
    },
    {
        "id": "6",
        "name": "Hello you"
    },
    {
        "id": "7",
        "name": "Hello you"
    },
    {
        "id": "8",
        "name": "Hello you"
    },
    {
        "id": "9",
        "name": "Hello you"
    }
]
```

# Packages description

- `reactiff-api` : the java API which contains codec and server public API.
- `reactiff-codec` : basic codecs : text, file and binary.
- `reactiff-codec-jackson` : json and smile codecs implemented with jackson.
- `reactiff-server` : the server implementation (wrapping handlers and filters with reactor-netty)
