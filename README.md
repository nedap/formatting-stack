# formatting-stack [![CircleCI](https://circleci.com/gh/nedap/formatting-stack.svg?style=svg&circle-token=581a4a0fa4b19f0ac5c7d90d494c9df0c34cee68)](https://circleci.com/gh/nedap/formatting-stack)

**formatting-stack** is a formatting/linting solution that is typically integrated with:

* your [Component](https://github.com/stuartsierra/component) (or [Integrant](https://github.com/weavejester/integrant), or bare [clojure.tools.namespace.repl](https://github.com/clojure/tools.namespace)) system
  * for instantaneous performance
    * no cold-starts!
  * and precise understanding of your codebase
    * no AST heuristics, no `eval` either
  * and a reasonable triggering frequency
    * so you don't format too frequently (format-on-save), or not frequently enough (git pre-commit hook)
* Git status/branch information
  * for some performance gains (typically only added/changed files will be processed)
  * and also for gradual formatting

As of today, it is integrated with:

  * [cljfmt](https://github.com/weavejester/cljfmt)
  * [how-to-ns](https://github.com/gfredericks/how-to-ns)
  * [eastwood](https://github.com/jonase/eastwood)
  * [clj-kondo](https://github.com/borkdude/clj-kondo)
    * Defaults to processing .cljs files only, given the overlap with Eastwood.
  * [refactor-nrepl](https://github.com/clojure-emacs/refactor-nrepl)
    * Used for "clean unused imports" functionality
  * [bikeshed](https://github.com/dakrone/lein-bikeshed)
    * Used for checking max column count
  * [all-my-files-should-end-with-exactly-one-newline-character](https://github.com/gfredericks/lein-all-my-files-should-end-with-exactly-one-newline-character)
    * Configurable, you can ensure either 0 or 1 ending newlines per file.

And it also bundles a few tiny linters of its own:

  * [loc-per-ns](https://github.com/nedap/formatting-stack/blob/debdab8129dae7779d390216490625a3264c9d2c/src/formatting_stack/linters/loc_per_ns.clj) warns if a given NS surpasses a targeted LOC count.
  * [ns-aliases](https://github.com/nedap/formatting-stack/blob/debdab8129dae7779d390216490625a3264c9d2c/src/formatting_stack/linters/ns_aliases.clj) warns if [Sierra's](https://stuartsierra.com/2015/05/10/clojure-namespace-aliases) aliasing guide is disregarded.

It is fully extensible: you can configure the bundled formatters, remove them, and/or add your own.

Each formatter makes full use of your CPU's cores.

## Smart code analysis

As mentioned, **formatting-stack** understands your codebase and its dependencies.
It knows which vars in the project are macros. It also reads the metadata of all function/macro vars.

It also associates `:style` metadata to your project's vars, in a clean manner, when needed.

Armed with those powers, we can do two nifty things:

* Inform cljfmt of indentation through metadata + config, using the [CIDER indent spec format](https://cider.readthedocs.io/en/latest/indent_spec/)
(by default, using an heuristic for cider->cljfmt format translation) or the cljfmt format (as a fallback).
  * Recommendation: use metadata **for your own code**, use config for **third-party code** (that may be awaiting for a pull request)
* Inform CIDER of indentation rules through config
  * CIDER understands either metadata or emacs-specific rules, but not config

You can find examples of how to do such configuration in the [wiki](https://github.com/nedap/formatting-stack/wiki/Indentation-examples).

## Graceful git strategies

Git integration is documented at `formatting-stack.strategies`.

The general intent is to make formatting:

* Efficient
  * don't process non-touched files
* Gradual
  * don't format the whole project at once
  * favor reviewable diffs - nobody can review (or learn from) whole-project diffs
* Safe
  * only format code that is completely staged by git
  * else any bug in formatting code could destroy your unsaved changes

## Installation

#### Coordinates

```clojure
[formatting-stack "1.0.1"]
```

**Also** you have to add the latest [cider-nrepl](https://clojars.org/cider/cider-nrepl).
* It's a transitive, non-automatically-fetched dependency of refactor-nrepl.
* Sadly we cannot just add it to our project.clj, since it would affect users of lower cider-nrepl versions.

### Component/Integrant integration

**formatting-stack** provides components that you can integrate into your system.

The provided components are fully configurable. See `formatting-stack.core`, `formatting-stack.component`, `formatting-stack.integrant`.

(Fear not about reading code. Any namespace here not ending in `impl.clj` is optimized for readability.)

### Reloaded Workflow integration

* If you use the Component component, then `com.stuartsierra.component.repl/reset` will use formatting-stack, applying all its formatters/linters.
* If you use the Integrant component, then `integrant.repl/reset` will use formatting-stack, applying all its formatters/linters.

The above can be good enough. However `reset`ting your system can be somewhat expensive,
and you may find yourself using `clojure.tools.namespace.repl/refresh` instead.

For that case, you can create some facility (e.g. shortcut, snippet) for the following code:

```clojure
(clojure.tools.namespace.repl/refresh :after 'formatting-stack.core/format!)
```

## Advanced configuration

If you want to add custom members to the component's `:formatters` (or `:strategies`, etc), a typical pattern would be:

```clojure
(formatting-stack.component/map->Formatter {:formatters (conj formatting-stack.defaults/default-formatters
                                                              my-custom-formatter)})
```

You can also pass `[]` for disabling a particular aspect:

```clojure
;; The default :formatters will be used, the default :linters will be omitted:
(formatting-stack.component/map->Formatter {:linters []})
```

There's no facility for finer-grained manipulations, e.g. removing only certain formatters, or adding a formatter at a certain position rather than at the end.

That's by design, to avoid intrincate DSLs or data structures.
If you need something finer-grained, you are encouraged to copy the contents of the `formatting-stack.defaults` ns to your project, adapting things as needed.
That ns is a deliberately thin and data-only one, with the precise purpose of being forked at no cost.

## [FAQ](https://github.com/nedap/formatting-stack/wiki/FAQ)

## License

Copyright © Nedap

This program and the accompanying materials are made available under the terms of the [Eclipse Public License 2.0](https://www.eclipse.org/legal/epl-2.0)
