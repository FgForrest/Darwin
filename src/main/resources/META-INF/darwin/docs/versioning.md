# Versioning rules

Darwin expects components to use [semantic versioning](https://semver.org/). Version can be composed of multiple parts
by using delimiters: .-_

You can use numbers or words in your versions. Numbers are compared by natural order, texts are compared by alphabet order
ignoring case. Numbers have bigger priority than words. Word `SNAPSHOT` is ignored in comparation, but when versions are
equal SNAPSHOT will be automatically resolved as lesser version than non-snapshot one.

***Example from test:** 1.1 > 1.0, 1.0-SNAPSHOT = 1.0, 1.1.1 > 1.1, 1.1-alfa > 1.1-beta, 1.1-alfa-2 > 1.1-alfa-1*