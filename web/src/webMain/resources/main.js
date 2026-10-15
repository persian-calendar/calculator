/*
 * The page: read the expression, evaluate it with the Kotlin bundle, and show the result,
 * the examples and a short history.
 *
 * The bundle hands over `evaluate(expression)`, which is the library's `eval`: a string in,
 * a string out, one line per statement, and a thrown error when the expression does not
 * parse. Everything else - the elements, the debounce, the history, the clipboard - is
 * here, so the Kotlin side stays a demonstration of the library.
 */
(function () {
	'use strict';

	var DEBOUNCE_MILLIS = 140;
	var HISTORY_LIMIT = 8;

	/* A few expressions that between them show what the evaluator does: operator
	   precedence and associativity, the functions and units, statements and variables,
	   and the symbolic differentiation and integration. */
	var EXAMPLES = [
		'2 + 2 * 2',
		'2 ^ 2 ^ 3',
		'sin(PI / 2)',
		'sin 90 deg',
		'7 / 5',
		'1d + 2h + 3m + 4s',
		'a = 3; a * 21',
		'diff(x ^ 2, x)',
		'integrate(x ^ 2, x)',
		'cbrt(8) + log2(8)',
	];

	function element(id) {
		var found = document.getElementById(id);
		if (!found) throw new Error('index.html has no #' + id);
		return found;
	}

	/* The library throws on an expression it cannot parse, and its message is the useful part
	   of that. What a thrown value looks like depends on the target - a real Error in
	   JavaScript, a Kotlin exception handed over by the WebAssembly runtime - so this takes
	   whatever arrives and finds the message, falling back to a plain sentence when there is
	   nothing readable in it. */
	function describe(error) {
		if (typeof error === 'string' && error.length > 0) return error;
		if (error && typeof error.message === 'string' && error.message.length > 0) return error.message;
		var text = error === undefined || error === null ? '' : String(error);
		if (text.length > 0 && text !== 'undefined' && text !== 'null' && text !== '[object Object]') {
			return text;
		}
		return 'Invalid expression.';
	}

	function start(evaluate) {
		var expressionInput = element('expression');
		var resultPanel = element('result');
		var lines = element('lines');
		var examples = element('examples');
		var historyList = element('history');
		var copyButton = element('copy');
		var clearButton = element('clear');

		/* What the page last evaluated, as separate lines, and whether it was an error. */
		var current = null;
		var history = [];

		function show(linesToShow, isError) {
			resultPanel.classList.toggle('is-error', isError);
			lines.textContent = '';
			linesToShow.forEach(function (line) {
				var paragraph = document.createElement('p');
				paragraph.className = 'result__line' + (line === '' ? ' result__line--empty' : '');
				paragraph.textContent = line === '' ? '—' : line;
				lines.appendChild(paragraph);
			});
			copyButton.disabled = isError || linesToShow.length === 0;
		}

		function onEvaluate() {
			var expression = expressionInput.value;
			/* eval returns one line per statement and '' when there is nothing to show,
			   so only an expression with something in it is worth remembering. */
			if (expression.trim() === '') {
				current = null;
				show([], false);
				return;
			}

			var value;
			try {
				value = evaluate(expression);
			} catch (error) {
				current = null;
				show([describe(error)], true);
				return;
			}

			var shown = value.split('\n');
			current = value;
			show(shown, false);
			remember(expression, shown[shown.length - 1]);
		}

		function remember(expression, value) {
			history = history.filter(function (entry) {
				return entry.expression !== expression;
			});
			history.unshift({ expression: expression, value: value });
			if (history.length > HISTORY_LIMIT) history.pop();
			renderHistory();
		}

		function renderHistory() {
			historyList.textContent = '';
			history.forEach(function (entry) {
				var item = document.createElement('li');
				var button = document.createElement('button');
				button.type = 'button';
				button.title = 'Put this expression back in the box';

				var expression = document.createElement('span');
				expression.className = 'history__expression';
				expression.textContent = entry.expression;

				var value = document.createElement('span');
				value.className = 'history__value';
				value.textContent = entry.value;

				button.appendChild(expression);
				button.appendChild(value);
				button.addEventListener('click', function () {
					expressionInput.value = entry.expression;
					update();
				});
				item.appendChild(button);
				historyList.appendChild(item);
			});
		}

		/* Redrawing on every keystroke is wasted work, so wait for a pause in typing. */
		var pending = null;
		function update() {
			if (pending) clearTimeout(pending);
			pending = setTimeout(onEvaluate, DEBOUNCE_MILLIS);
		}

		expressionInput.addEventListener('input', update);
		expressionInput.addEventListener('keydown', function (event) {
			/* Ctrl/Cmd+Enter evaluates at once instead of waiting for the pause. */
			if (event.key !== 'Enter' || !(event.ctrlKey || event.metaKey)) return;
			event.preventDefault();
			if (pending) clearTimeout(pending);
			onEvaluate();
		});

		EXAMPLES.forEach(function (example) {
			var button = document.createElement('button');
			button.type = 'button';
			button.textContent = example;
			button.addEventListener('click', function () {
				expressionInput.value = example;
				update();
				expressionInput.focus();
			});
			examples.appendChild(button);
		});

		copyButton.addEventListener('click', function () {
			if (current === null) return;
			navigator.clipboard.writeText(current).then(function () {
				copyButton.textContent = 'Copied';
				setTimeout(function () {
					copyButton.textContent = 'Copy result';
				}, 900);
			}, function (error) {
				console.error('Failed to copy the result: ', error);
			});
		});

		clearButton.addEventListener('click', function () {
			expressionInput.value = '';
			history = [];
			renderHistory();
			onEvaluate();
			expressionInput.focus();
		});

		onEvaluate();
	}

	window.calculatorReady = start;
})();
