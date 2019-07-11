# full.core

[![Clojars Project](https://img.shields.io/clojars/v/fullcontact/full.core.svg)](https://clojars.org/fullcontact/full.core)
[![Build Status](https://travis-ci.org/fullcontact/full.core.svg?branch=master)](https://travis-ci.org/fullcontact/full.core)

Full.core is the core library for Clojure and ClojureScript services at [FullContact](//fullcontact.com).

It contains the following:

* Config management
* Logging
* Sugar - extensions to clojure.core and other common libraries

* edn helpers (Clojure only)
* `clj-time` wrapper (Clojure only)


## Config management

With `full.core.config` you can manage yaml configurations for your app.

If you have the following config:

```yaml
app: facebookForCats
hosts:
  - host1
  - host2
  - host3
parent:
  child: value
secret-key: "${SECRET_KEY}"
```

We can use config as follows:

```clojure
(def app-name (opt :app))   ; @app-name will be "facebookForCats"
(def child (opt [:parent :child])) ; @child will be "value"
(def hosts (opt :hosts :mapper set))  ; @hosts will be #{"host1" "host2" "host3"}
(def space-cakes (opt :space-cakes :default nil)) ; @space-cakes will be nil
(def oh-no (opt [:this :will :raise]))  ; @oh-no will raise RuntimeException
```

Path to config file can be set via `-c path/to/file.yaml` or as `FULL_CONFIG`
env variable. config loader will default to reading `dev.yaml` in project's
root directory if no explicit config path is provided.

You can load values from environment variables with the "${VAR_NAME}" syntax.
They will behave like regular strings when loaded via `opt`.

## Logging

`full.core.log` is using slf4j under the hood & logging config can be loaded
via `(full.core.log/configure)`. Path of the XML logging config can be set via
`log-config` field in your YAML config.

`full.core.log` provides logging with MDC contexts:

```
(log/with-mdc {:foo "bar" :baz "foo"} "Message")
```

`full.core.log/do-info` and `full.core.log/do-debug` will log all arguments
and return the value of last (it works similar to Haskell's [Debug.Trace](https://hackage.haskell.org/package/base-4.9.0.0/docs/Debug-Trace.html)).


## Core extensions

* `full.core.sugar` contains extensions to standard data types and is similar to
[plumbing](https://github.com/plumatic/plumbing) and friends.

* `full.core.time` contains extensions to [clj-time](https://github.com/clj-time/clj-time).
