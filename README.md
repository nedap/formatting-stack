# formatting-stack [![CircleCI](https://circleci.com/gh/nedap/formatting-stack.svg?style=svg&circle-token=581a4a0fa4b19f0ac5c7d90d494c9df0c34cee68)](https://circleci.com/gh/nedap/formatting-stack)

**formatting-stack** is a formatting/linting solution that can be integrated with:

* your [Component](https://github.com/stuartsierra/component) (or [Integrant](https://github.com/weavejester/integrant), or bare [tools.namespace](https://github.com/clojure/tools.namespace)) system
  * for instantaneous performance
    * no cold-starts!
  * and precise understanding of your codebase
    * powered by Clojure's introspection capabilities (reified vars, namespaces), and occasionally `eval`
  * and a reasonable triggering frequency
    * so you don't format too frequently (format-on-save), or not frequently enough (git pre-commit hook)
* Git status/branch information
  * for some performance gains (typically only added/changed files will be processed)
  * and also for gradual formatting
* Anything you want
  * A vanilla repl, Lein task, CI workflow...

As of today, it is integrated with:

  * [cljfmt](https://github.com/weavejester/cljfmt)
  * [how-to-ns](https://github.com/gfredericks/how-to-ns)
  * [eastwood](https://github.com/jonase/eastwood)
  * [clj-kondo](https://github.com/borkdude/clj-kondo)
    * By default, both Eastwood and Kondo are enabled, each having some linters disabled, filling each other's gaps.
  * [refactor-nrepl](https://github.com/clojure-emacs/refactor-nrepl)
    * Used for "clean unused imports" functionality
  * [all-my-files-should-end-with-exactly-one-newline-character](https://github.com/gfredericks/lein-all-my-files-should-end-with-exactly-one-newline-character)
    * Configurable, you can ensure either 0 or 1 ending newlines per file.

And it also bundles a few tiny linters of its own:

  * [loc-per-ns](https://github.com/nedap/formatting-stack/blob/debdab8129dae7779d390216490625a3264c9d2c/src/formatting_stack/linters/loc_per_ns.clj) warns if a given NS surpasses a targeted LOC count.
  * [ns-aliases](https://github.com/nedap/formatting-stack/blob/debdab8129dae7779d390216490625a3264c9d2c/src/formatting_stack/linters/ns_aliases.clj) warns if [Sierra's](https://stuartsierra.com/2015/05/10/clojure-namespace-aliases) aliasing guide is disregarded.
  * [one-resource-per-ns](https://github.com/nedap/formatting-stack/blob/master/src/formatting_stack/linters/one_resource_per_ns.clj) warns if a Clojure namespace is defined in more than one file.
  * [line-length](https://github.com/nedap/formatting-stack/blob/f1cf4206399a77a83fde4140095d4c59c10b1605/src/formatting_stack/linters/line_length.clj) warns when a given max line length is surpassed.

It is fully extensible: you can configure the bundled formatters, remove them, and/or add your own.

Whenever it's safe, each formatter/linter will make full use of your CPU's cores. 

Linters' reports are presented under a unified format. 

## Smart code analysis

As mentioned, **formatting-stack** understands your codebase and its dependencies.
It knows which vars in the project are macros. It also reads the metadata of all function/macro vars.

It also associates `:style` metadata to your project's vars, in a clean manner, when needed.

Armed with those powers, we can do two nifty things:

* Inform cljfmt of indentation through metadata + config, using the [CIDER indent spec format](https://docs.cider.mx/cider/indent_spec.html)
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

...that's the default Git `strategy` anyway, apt for repl-driven development. You are free to supply an alternative strategy.

Commonly needed alternative strategies are offered/documented in [branch-formatter](https://github.com/nedap/formatting-stack/blob/0d78f726555db175aa446f4a0a9d2e289cfdd540/src/formatting_stack/branch_formatter.clj) and [project-formatter](https://github.com/nedap/formatting-stack/blob/0d78f726555db175aa446f4a0a9d2e289cfdd540/src/formatting_stack/project_formatter.clj).

## Consolidated reporting

As you can see in the screenshot, **formatting-stack** presents linters' outputs under a hierarchical, file-grouped format.

<img width="710" alt="Screenshot 2020-02-19 at 07 04 38" src="https://user-images.githubusercontent.com/1162994/74806403-2aaa9700-52e6-11ea-8088-b073d82e2879.png">

Alternative reporters can be found in `./src/formatting_stack/reporters`, such as
 - `formatting-stack.pretty-line-printer` offers more concise output
 - `formatting-stack.file-writer` offers a file-output instead of stdout

## Installation

#### Coordinates

```clojure
[formatting-stack "4.6.0"]
```

**Also** you might have to add the [refactor-nrepl](https://github.com/clojure-emacs/refactor-nrepl) dependency.
  * If you use tooling like CIDER, typically this dependency will be already injected into your classpath, so no action required in this case.
  * Else, please add the latest version to your project (or personal [profile](https://github.com/technomancy/leiningen/blob/072dcd62dea0ea46413cf938878e2d31b76357c9/doc/PROFILES.md)).
  * If this dependency isn't added, formatting-stack will degrade gracefully, using slightly fewer formatters/linters.

### Reloaded Workflow integration

* If you use the Component component, then `com.stuartsierra.component.repl/reset` will use formatting-stack, applying all its formatters/linters.
  * You can find a working sample setup in [component_repl.clj](https://github.com/nedap/formatting-stack/blob/master/test-resources/component_repl.clj).
* If you use the Integrant component, then `integrant.repl/reset` will use formatting-stack, applying all its formatters/linters.
  * You can find a working sample setup in [integrant_repl.clj](https://github.com/nedap/formatting-stack/blob/master/test-resources/integrant_repl.clj).

The above can be good enough. However `reset`ting your system can be somewhat expensive,
and you may find yourself using `clojure.tools.namespace.repl/refresh` instead.

For that case, you can create some facility (e.g. shortcut, snippet) for the following code:

```clojure
(clojure.tools.namespace.repl/refresh :after 'formatting-stack.core/format!)
```

### Vanilla integration

[`formatting-stack.core/format!`](https://github.com/nedap/formatting-stack/blob/0d78f726555db175aa446f4a0a9d2e289cfdd540/src/formatting_stack/core.clj#L49) is a plain function, considered a public API, that is safe to invoke over REPL, a script, or anything you please.

> See also: [`format-and-lint-branch!`](https://github.com/nedap/formatting-stack/blob/5d66e2adffd1696af8b020c56d33d443b299aabd/src/formatting_stack/branch_formatter.clj#L84), [`format-and-lint-project!`](https://github.com/nedap/formatting-stack/blob/5d66e2adffd1696af8b020c56d33d443b299aabd/src/formatting_stack/project_formatter.clj#L84).

## Advanced configuration

If you want to add custom members to the `format!` options (namely: `:formatters`, or `:strategies`, etc), a typical pattern would be:

```clojure
(formatting-stack.core/format! :formatters (conj formatting-stack.defaults/default-formatters my-custom-formatter))
```

You can also pass `[]` for disabling a particular aspect:

```clojure
;; The default :formatters will be used, no :linters will be run:
(formatting-stack.core/format! :linters [])
```

...And you can also override specific parameters (like max line length from 130 to 80) in a fine-grained manner, as documented in [customization_example.clj](https://github.com/nedap/formatting-stack/blob/master/test-resources/customization_example.clj).

## [FAQ](https://github.com/nedap/formatting-stack/wiki/FAQ)

## License

Copyright © Nedap

This program and the accompanying materials are made available under the terms of the [Eclipse Public License 2.0](https://www.eclipse.org/legal/epl-2.0).
