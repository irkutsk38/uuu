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
        # 👇 ВАЖНО: сразу переходим в EvaluateOrder
        script:
            $reactions.transition("/EvaluateOrder");

    state: GlobalInput
        q: *
        script:
            if (!$session.order) {
                $session.order = {topping: null, size: null, dough: null, address: null, price: 0};
            }
            var text = String($request.query || "").toLowerCase();
            var recognized = false;

            // 1. Адрес (приоритет — если есть ключевые слова адреса)
            if (/ул|улица|дом|д\.|кв|квартира|проспект|пр-т|пр\./i.test(text)) {
                $session.order.address = $request.query;
                recognized = true;
            }

            // 2. Начинка
            if (text.indexOf("пепперони") !== -1) {
                $session.order.topping = {name: "Пепперони", base_price: 450};
                recognized = true;
            } else if (text.indexOf("маргарита") !== -1) {
                $session.order.topping = {name: "Маргарита", base_price: 400};
                recognized = true;
            } else if (text.indexOf("гавайская") !== -1) {
                $session.order.topping = {name: "Гавайская", base_price: 500};
                recognized = true;
            }

            // 3. Размер
            if (/больш|40/i.test(text)) {
                $session.order.size = {name: "Большая", price_modifier: 1.5};
                recognized = true;
            } else if (/маленьк|30/i.test(text)) {
                $session.order.size = {name: "Маленькая", price_modifier: 1};
                recognized = true;
            }

            // 4. Тесто
            if (/тонк/i.test(text)) {
                $session.order.dough = {name: "Тонкое"};
                recognized = true;
            } else if (/традиц/i.test(text)) {
                $session.order.dough = {name: "Традиционное"};
                recognized = true;
            }

            // Считаем цену
            if ($session.order.topping && $session.order.size) {
                $session.order.price = $session.order.topping.base_price * $session.order.size.price_modifier;
            }

            if (recognized) {
                $reactions.transition("/EvaluateOrder");
            } else {
                $reactions.answer("Не понял. Назовите начинку (Пепперони, Маргарита, Гавайская), размер (Маленькая/Большая), тесто (Тонкое/Традиционное) или адрес доставки.");
            }

    state: EvaluateOrder
        script:
            if (!$session.order.topping) { $reactions.transition("/AskTopping"); }
            else if (!$session.order.size) { $reactions.transition("/AskSize"); }
            else if (!$session.order.dough) { $reactions.transition("/AskDough"); }
            else if (!$session.order.address) { $reactions.transition("/AskAddress"); }
            else { $reactions.transition("/ConfirmOrder"); }

    state: AskTopping
        a: Какую начинку вы предпочитаете? (Пепперони, Маргарита, Гавайская). Также можете сразу назвать размер, тесто или адрес.

    state: AskSize
        a: Какой размер пиццы? (Маленькая 30см / Большая 40см). Также можете назвать начинку, тесто или адрес.

    state: AskDough
        a: Какое тесто? (Тонкое / Традиционное). Также можете назвать начинку, размер или адрес.

    state: AskAddress
        a: Назовите адрес доставки.

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