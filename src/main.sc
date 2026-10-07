theme: /
    state: Start
        q!: $regex</start>
        intent!: /Приветствие
        script:
            $session.order = {
                topping: null,
                size: null,
                dough: null,
                address: null,
                price: 0
            };
        a: Здравствуйте! Я бот для заказа пиццы. Какую пиццу вы бы хотели заказать? (Пепперони, Маргарита, Гавайская)

    state: SetTopping
        script:
            if (!$session.order) {
                $session.order = {topping: null, size: null, dough: null, address: null, price: 0};
            }
            var text = String($request.query || "").toLowerCase();
            if (text.indexOf("пепперони") !== -1) {
                $session.order.topping = {name: "Пепперони", base_price: 450};
            } else if (text.indexOf("маргарита") !== -1) {
                $session.order.topping = {name: "Маргарита", base_price: 400};
            } else if (text.indexOf("гавайская") !== -1) {
                $session.order.topping = {name: "Гавайская", base_price: 500};
            }
            $reactions.transition("/EvaluateOrder");

    state: FillOrder
        intent!: /OrderPizza
        script:
            if (!$session.order) {
                $session.order = {topping: null, size: null, dough: null, address: null, price: 0};
            }
            var slots = $parseTree._OrderPizza;
            if (slots) {
                if (slots.topping) $session.order.topping = slots.topping;
                if (slots.size) $session.order.size = slots.size;
                if (slots.dough) $session.order.dough = slots.dough;
                if (slots.address) $session.order.address = slots.address;
            }
            if ($session.order.topping && $session.order.size) {
                $session.order.price = $session.order.topping.base_price * $session.order.size.price_modifier;
            }
            $reactions.transition("/EvaluateOrder");

    state: EvaluateOrder
        script:
            if (!$session.order.topping) { $reactions.transition("/AskTopping"); }
            else if (!$session.order.size) { $reactions.transition("/AskSize"); }
            else if (!$session.order.dough) { $reactions.transition("/AskDough"); }
            else if (!$session.order.address) { $reactions.transition("/AskAddress"); }
            else { $reactions.transition("/ConfirmOrder"); }

    state: AskTopping
        a: Какую начинку вы предпочитаете? (Пепперони, Маргарита, Гавайская). Также можете сразу назвать размер, тесто или адрес.
        state: LocalTopping
            q: * @PizzaTopping *
            script:
                $session.order.topping = $parseTree._PizzaTopping;
                if ($session.order.topping && $session.order.size) {
                    $session.order.price = $session.order.topping.base_price * $session.order.size.price_modifier;
                }
                $reactions.transition("/EvaluateOrder");
        state: LocalToppingText
            q: *
            script:
                var text = String($request.query || "").toLowerCase();
                if (text.indexOf("пепперони") !== -1) {
                    $session.order.topping = {name: "Пепперони", base_price: 450};
                    $reactions.transition("/EvaluateOrder");
                } else if (text.indexOf("маргарита") !== -1) {
                    $session.order.topping = {name: "Маргарита", base_price: 400};
                    $reactions.transition("/EvaluateOrder");
                } else if (text.indexOf("гавайская") !== -1) {
                    $session.order.topping = {name: "Гавайская", base_price: 500};
                    $reactions.transition("/EvaluateOrder");
                } else if (/ул|улица|дом|д\.|кв|квартира|проспект|пр-т|пр\./i.test(text)) {
                    $session.order.address = text;
                    $reactions.transition("/EvaluateOrder");
                } else if (/больш|40/i.test(text)) {
                    $session.order.size = {name: "Большая", price_modifier: 1.5};
                    $reactions.transition("/EvaluateOrder");
                } else if (/маленьк|30/i.test(text)) {
                    $session.order.size = {name: "Маленькая", price_modifier: 1};
                    $reactions.transition("/EvaluateOrder");
                } else if (/тонк|традиц/i.test(text)) {
                    $session.order.dough = {name: /тонк/i.test(text) ? "Тонкое" : "Традиционное"};
                    $reactions.transition("/EvaluateOrder");
                } else {
                    $reactions.answer("Не понял. Назовите начинку (Пепперони, Маргарита, Гавайская), размер, тесто или адрес.");
                }

    state: AskSize
        a: Какой размер пиццы? (Маленькая 30см / Большая 40см). Также можете назвать начинку, тесто или адрес.
        state: LocalSizeText
            q: *
            script:
                var text = String($request.query || "").toLowerCase();
                if (/больш|40/i.test(text)) {
                    $session.order.size = {name: "Большая", price_modifier: 1.5};
                } else if (/маленьк|30/i.test(text)) {
                    $session.order.size = {name: "Маленькая", price_modifier: 1};
                } else if (text.indexOf("пепперони") !== -1) {
                    $session.order.topping = {name: "Пепперони", base_price: 450};
                } else if (text.indexOf("маргарита") !== -1) {
                    $session.order.topping = {name: "Маргарита", base_price: 400};
                } else if (text.indexOf("гавайская") !== -1) {
                    $session.order.topping = {name: "Гавайская", base_price: 500};
                } else if (/тонк|традиц/i.test(text)) {
                    $session.order.dough = {name: /тонк/i.test(text) ? "Тонкое" : "Традиционное"};
                } else if (/ул|улица|дом|д\.|кв|квартира|проспект|пр-т|пр\./i.test(text)) {
                    $session.order.address = text;
                } else {
                    $reactions.answer("Не понял. Назовите размер (Маленькая/Большая), начинку, тесто или адрес.");
                }
                if ($session.order.topping && $session.order.size) {
                    $session.order.price = $session.order.topping.base_price * $session.order.size.price_modifier;
                }
                $reactions.transition("/EvaluateOrder");

    state: AskDough
        a: Какое тесто? (Тонкое / Традиционное). Также можете назвать начинку, размер или адрес.
        state: LocalDoughText
            q: *
            script:
                var text = String($request.query || "").toLowerCase();
                if (/тонк/i.test(text)) {
                    $session.order.dough = {name: "Тонкое"};
                } else if (/традиц/i.test(text)) {
                    $session.order.dough = {name: "Традиционное"};
                } else if (text.indexOf("пепперони") !== -1) {
                    $session.order.topping = {name: "Пепперони", base_price: 450};
                } else if (text.indexOf("маргарита") !== -1) {
                    $session.order.topping = {name: "Маргарита", base_price: 400};
                } else if (text.indexOf("гавайская") !== -1) {
                    $session.order.topping = {name: "Гавайская", base_price: 500};
                } else if (/больш|40/i.test(text)) {
                    $session.order.size = {name: "Большая", price_modifier: 1.5};
                } else if (/маленьк|30/i.test(text)) {
                    $session.order.size = {name: "Маленькая", price_modifier: 1};
                } else if (/ул|улица|дом|д\.|кв|квартира|проспект|пр-т|пр\./i.test(text)) {
                    $session.order.address = text;
                } else {
                    $reactions.answer("Не понял. Назовите тесто (Тонкое/Традиционное), начинку, размер или адрес.");
                }
                if ($session.order.topping && $session.order.size) {
                    $session.order.price = $session.order.topping.base_price * $session.order.size.price_modifier;
                }
                $reactions.transition("/EvaluateOrder");

    state: AskAddress
        a: Назовите адрес доставки.
        state: LocalAddress
            q: *
            script:
                $session.order.address = $request.query;
                $reactions.transition("/EvaluateOrder");

    state: ConfirmOrder
        script:
            var msg = "Проверьте ваш заказ:\n" +
                      "• Пицца: " + $session.order.topping.name + "\n" +
                      "• Размер: " + $session.order.size.name + "\n" +
                      "• Тесто: " + $session.order.dough.name + "\n" +
                      "• Адрес: " + $session.order.address + "\n" +
                      "• Итоговая цена: " + $session.order.price + " руб.\n\n" +
                      "Всё верно?";
            $reactions.answer(msg);
            $reactions.buttons([
                {text: "Да, подтверждаю", transition: "/ProcessingOrder"},
                {text: "Изменить заказ", transition: "/ChangeOrder"},
                {text: "Сбросить всё", transition: "/ResetOrder"}
            ]);

    state: ChangeOrder
        a: Что изменить? (Начинку / Размер / Тесто / Адрес)
        buttons:
            "Начинку" -> /AskTopping
            "Размер" -> /AskSize
            "Тесто" -> /AskDough
            "Адрес" -> /AskAddress

    state: ProcessingOrder
        a: Спасибо! Ваш заказ принят и передан на кухню. Готовим вашу пиццу!
        script:
            $session.order = null;

    state: ResetOrder
        q!: * (сброс*|отмена|заново|очистить) *
        script:
            $session.order = {
                topping: null,
                size: null,
                dough: null,
                address: null,
                price: 0
            };
        a: Данные заказа сброшены. Чем я могу помочь?
        buttons:
            "Начать сначала" -> /Start

    state: CatchAll
        event!: noMatch
        a: Я вас не совсем понял. Пожалуйста, уточните детали заказа или нажмите /start.