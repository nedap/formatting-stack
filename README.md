# formatting-stack

[Clojars](https://clojars.org/formatting-stack)

**formatting-stack** is a formatting/linting solution that is typically integrated with:

* your [Component](https://github.com/stuartsierra/component) (or [Integrant](https://github.com/weavejester/integrant), or bare [clojure.tools.namespace.repl](https://github.com/clojure/tools.namespace)) system
  * for instantaneous performance
    * no cold-starts!
  * and precise understanding of your codebase
    * no AST heuristics, no `eval` either
* Git status/branch information
  * for some performance gains (only added/changed files will be processed)
  * and also for gradual formatting

As of today, it is integrated with:

  * [cljfmt](https://github.com/weavejester/cljfmt)
  * [how-to-ns](https://github.com/gfredericks/how-to-ns)
  * [eastwood](https://github.com/jonase/eastwood)
  * [refactor-nrepl](https://github.com/clojure-emacs/refactor-nrepl)

It is fully extensible: you can configure the bundled formatters, remove them, and/or add your own.

## Smart

As mentioned, **formatting-stack** understands your codebase and its dependencies.
It knows which vars in the project are macros. It also reads the metadata of all function/macro vars.

It also associates `:style` metadata to your project's vars, in a clean manner, when needed.

Armed with those powers, we can do two nifty things:

* Inform cljfmt of indentation through metadata + config, using the [CIDER indent spec format](https://cider.readthedocs.io/en/latest/indent_spec/)
(by default, using an heuristic for cider->cljfmt format translation) or the cljfmt format (as a fallback).
  * Recommendation: use metadata **for your own code**, use config for **third-party code** (that may be awaiting for a pull request)
* Inform CIDER of indentation rules through config
  * CIDER understands either metadata or emacs-specific rules, but not config

### Metadata examples

For most macros/functions, adding `:style/indent n` as usual will suffice.
formatting-stack will understand it (translating it for cljfmt, provided the spec is simply a number),
and any spec-compliant editor will understand it too.

```clojure
(defmacro render
  {:style/indent 1} ;; CIDER will use `1`, cljfmt will receive the translated value of [[:block 1]]
  [options & body]
  ...)
```     

By default, `:block` indentation is assumed for cljfmt (see its README or [these examples](https://github.com/weavejester/cljfmt/blob/806e43b7a7d4e22b831d796f107f135d8efc986a/cljfmt/resources/cljfmt/indents/clojure.clj) for a primer on `:format` / `:inner`). You can use `:inner` via `:style.cljfmt/type`:

```clojure
(defmacro render
  {:style/indent 1 ;; CIDER will use `1`
   :style.cljfmt/type :inner} ;; cljfmt will receive the translated value of [[:inner 1]]
  [options & body]
  ...)
```

As said, formatting-stack can only translate simple, single-digit specs like `{:style/indent 1}` for cljfmt.
If you need anything more complex, use `:style.cljfmt/indent`. The values will be passed verbatim to cljfmt.

```clojure
(defmacro render
  {:style/indent 1 ;; CIDER will use `1`
   :style.cljfmt/indent [[:block 2] [:inner 1]]} ;; cljfmt will use `[[:block 2] [:inner 1]]`
  [...]
  ...)
```

If you want to avoid the situation where you have to author complex indentation specs in two formats, I'd recommend:

* avoid macros that parse the arguments in compile time (a trick occasionally found in the wild).
  * Use standard Clojure calling conventions and facilities (e.g. destructuring) 
* avoid superfluous multi-arity signatures: just always accept an options argument (`{}` or `& {}`, your choice)
  * Or in any case avoid multi-arity signatures where the same positional argument can have different meanings depending on the arity. 

## Config examples

As mentioned, you should favor metadata over config. Anyway, the config format is simply a map of symbols to specs:

```
'{fulcro-spec.core/assertions {:style/indent 0
                               :style.cljfmt/type :inner}}
```

The specs have the same semantics described in the previous section. 

This config is passed to `formatting-stack.core/format!`, either via Component/Integrant or by direct invocation.

## Component/Integrant integration

**formatting-stack** provides components that you can integrate into your system.

The provided components are fully configurable. See `formatting-stack.core`, `formatting-stack.component`, `formatting-stack.integrant`
(fear not about reading code. Any namespace here not ending in `impl.clj` is optimized for readability).

## Reloaded Workflow integration

* If you use the Component component, then `com.stuartsierra.component.repl/reset` will use formatting-stack. 
* If you use the Integrant component, then `integrant.repl/reset` will use formatting-stack.

The above can be good enough. However `reset`ting your system can be somewhat expensive,
and you may find yourself using `clojure.tools.namespace.repl/refresh` instead.

For that case, you can create some facility (e.g. shortcut, snippet) for the following code:

```clojure
(clojure.tools.namespace.repl/refresh :after 'formatting-stack.core/format!)
```

## Git strategies

See `formatting-stack.strategies`.

The general intent is to make formatting:

* **efficient**
  * don't process non-touched files
* **gradual**
  * don't format the whole project at once
  * favor reviewable diffs - nobody can review whole-project diffs
* **safe**
  * only format code that is completely staged by git
  * else any bug in formatting code could destroy your unsaved changes

## I don't like this indentation!

**formatting-stack** doesn't introduce any creative formatting: it merely applies cljfmt,
which in turn follows quite closely the [Clojure Style Guide](https://github.com/bbatsov/clojure-style-guide).

It's obviously a good goal to adhere to standards and majorities.

Also, when it comes to formatting it's worth considering that you might be wrong:

> Who knows everything? -[Rich Hickey](https://github.com/matthiasn/talk-transcripts/commit/b3a1cdbb7480787d182d91b5d6921f7b9bc479ce#diff-7d9f1a837de37c2fa535dc0fd101220fR463)

All IDEs/editors have quirks. It's very easy to get attached to them,
and retrofit those quirks into made-up rules that only make sense to you (or a minority).

If you're unfamiliar with the traditional Lisp indentation, as standardized by cljfmt/clojure-style-guide,
you'll likely end up finding that having fine-grained rules which distinguish macro and function indentation
in fact makes code more readable. It's just so useful to distinguish between functions and macros at a glance!

## Roadmap

* ClojureScript support
* More formatters
* Linters!

## License

Copyright © Nedap

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
