theme: /
    # Старт диалога
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
        a: Здравствуйте! Я бот для заказа пиццы. Какую пиццу вы бы хотели заказать?
        buttons:
            "Пепперони" -> /SetTopping
            "Маргарита" -> /SetTopping
            "Гавайская" -> /SetTopping

    # Установка начинки из кнопки
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

    # Заполнение параметров из интента
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

    # Оценка собранных данных
    state: EvaluateOrder
        script:
            if (!$session.order.topping) { $reactions.transition("/AskTopping"); }
            else if (!$session.order.size) { $reactions.transition("/AskSize"); }
            else if (!$session.order.dough) { $reactions.transition("/AskDough"); }
            else if (!$session.order.address) { $reactions.transition("/AskAddress"); }
            else { $reactions.transition("/ConfirmOrder"); }

    # Уточняющие вопросы
    state: AskTopping
        a: Какую начинку/вид пиццы вы предпочитаете?
        buttons:
            "Пепперони"
            "Маргарита"
            "Гавайская"
        state: LocalTopping
            q: * @PizzaTopping *
            script:
                $session.order.topping = $parseTree._PizzaTopping;
                if ($session.order.topping && $session.order.size) {
                    $session.order.price = $session.order.topping.base_price * $session.order.size.price_modifier;
                }
                $reactions.transition("/EvaluateOrder");
        state: LocalSizeFromTopping
            q: * @PizzaSize *
            script:
                $session.order.size = $parseTree._PizzaSize;
                if ($session.order.topping && $session.order.size) {
                    $session.order.price = $session.order.topping.base_price * $session.order.size.price_modifier;
                }
                $reactions.transition("/EvaluateOrder");
        state: LocalDoughFromTopping
            q: * @PizzaDough *
            script:
                $session.order.dough = $parseTree._PizzaDough;
                $reactions.transition("/EvaluateOrder");
        state: LocalAddressFromTopping
            q: $regex<.*(ул|улица|дом|д\.|кв|квартира|проспект|пр-т|пр\.).*
            script:
                $session.order.address = $request.query;
                $reactions.transition("/EvaluateOrder");

    state: AskSize
        a: Какой размер пиццы вам приготовить?
        buttons:
            "Маленькая (30см)"
            "Большая (40см)"
        state: LocalSize
            q: * @PizzaSize *
            script:
                $session.order.size = $parseTree._PizzaSize;
                if ($session.order.topping && $session.order.size) {
                    $session.order.price = $session.order.topping.base_price * $session.order.size.price_modifier;
                }
                $reactions.transition("/EvaluateOrder");
        state: LocalToppingFromSize
            q: * @PizzaTopping *
            script:
                $session.order.topping = $parseTree._PizzaTopping;
                if ($session.order.topping && $session.order.size) {
                    $session.order.price = $session.order.topping.base_price * $session.order.size.price_modifier;
                }
                $reactions.transition("/EvaluateOrder");
        state: LocalDoughFromSize
            q: * @PizzaDough *
            script:
                $session.order.dough = $parseTree._PizzaDough;
                $reactions.transition("/EvaluateOrder");
        state: LocalAddressFromSize
            q: $regex<.*(ул|улица|дом|д\.|кв|квартира|проспект|пр-т|пр\.).*
            script:
                $session.order.address = $request.query;
                $reactions.transition("/EvaluateOrder");

    state: AskDough
        a: Какое тесто использовать: тонкое или традиционное?
        buttons:
            "Тонкое"
            "Традиционное"
        state: LocalDough
            q: * @PizzaDough *
            script:
                $session.order.dough = $parseTree._PizzaDough;
                $reactions.transition("/EvaluateOrder");
        state: LocalToppingFromDough
            q: * @PizzaTopping *
            script:
                $session.order.topping = $parseTree._PizzaTopping;
                if ($session.order.topping && $session.order.size) {
                    $session.order.price = $session.order.topping.base_price * $session.order.size.price_modifier;
                }
                $reactions.transition("/EvaluateOrder");
        state: LocalSizeFromDough
            q: * @PizzaSize *
            script:
                $session.order.size = $parseTree._PizzaSize;
                if ($session.order.topping && $session.order.size) {
                    $session.order.price = $session.order.topping.base_price * $session.order.size.price_modifier;
                }
                $reactions.transition("/EvaluateOrder");
        state: LocalAddressFromDough
            q: $regex<.*(ул|улица|дом|д\.|кв|квартира|проспект|пр-т|пр\.).*
            script:
                $session.order.address = $request.query;
                $reactions.transition("/EvaluateOrder");

    state: AskAddress
        a: Назовите, пожалуйста, адрес доставки.
        state: LocalAddress
            q: *
            script:
                $session.order.address = $request.query;
                $reactions.transition("/EvaluateOrder");

    # Подтверждение
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
        a: Что бы вы хотели изменить в заказе?
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