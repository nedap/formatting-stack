# formatting-stack

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
  * [refactor-nrepl](https://github.com/clojure-emacs/refactor-nrepl)
    * Used for "clean unused imports" functionality
  * [bikeshed](https://github.com/dakrone/lein-bikeshed)
    * Used for checking max column count
  * [all-my-files-should-end-with-exactly-one-newline-character](https://github.com/gfredericks/lein-all-my-files-should-end-with-exactly-one-newline-character)
    * Configurable, you can ensure either 0 or 1 ending newlines per file.

It is fully extensible: you can configure the bundled formatters, remove them, and/or add your own.

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

[Clojars](https://clojars.org/formatting-stack)

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

## License

Copyright © Nedap

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
