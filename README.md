
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

        try (ReactiveHttpServer server = ReactiveHttpServer.create()
                .protocols(HttpProtocol.HTTP11)
                .codecManager(codecManager)
                .port(3000)
                .build()) {

            server.addReactiveFilter(DefaultFilters.cors(
                    ImmutableSet.of("*"),
                    ImmutableSet.of("X-UserHeader"),
                    ImmutableSet.of("GET")
                    , true,
                    -1
            ));
            server.addReactiveHandler(new PojoHandler());
            server.start();

            Flux<Pojo> response = HttpClient.create()
                    .get()
                    .uri("http://localhost:3000/pojo")
                    .response(codecManager.decodeAsFlux(Pojo.class));

            response.subscribe(System.out::println);

            System.out.println(String.format("Received %d results", response.count().block()));
        }
    }

}

```

``````

# Packages description

- `reactiff-api` : the java API which contains codec and server public API.
- `reactiff-codec` : basic codecs : text, file and binary.
- `reactiff-codec-jackson` : json and smile codecs implemented with jackson.
- `reactiff-server` : the server implementation (wrapping handlers and filters with reactor-netty)
