require: order.js

theme: /

    state: Start
        q!: $regex</start>
        script:
            pizzaReset($session);
        a: Здравствуйте! Я бот для заказа пиццы. Можно назвать начинку, размер, основу и адрес в любом порядке или одной фразой.
        go!: /EvaluateOrder

    state: Hello
        intent!: /привет
        script:
            pizzaEnsure($session);
        a: Здравствуйте! Помогу заказать пиццу. Уже указанные параметры сохранены.
        go!: /EvaluateOrder

    state: Bye
        intent!: /пока
        a: До свидания! Будем рады вашему следующему заказу.

    state: OrderInput
        intent!: /OrderPizza
        go!: /ApplyParameters

    state: GetTopping
        intent!: /ToppingIntent
        go!: /ApplyParameters

    state: GetSize
        intent!: /SizeIntent
        go!: /ApplyParameters

    state: GetDough
        intent!: /DoughIntent
        go!: /ApplyParameters

    state: GetAddress
        intent!: /AddressIntent
        go!: /ApplyParameters

    state: ApplyParameters
        script:
            var result = pizzaApply($session, $parseTree, $entities, $request.query);
            if (result.errors.length) $reactions.answer(result.errors.join(' '));
            if (result.changed.length) $reactions.answer('Параметры заказа сохранены.');
        go!: /EvaluateOrder

    state: EvaluateOrder
        script:
            $reactions.transition(pizzaNext($session));

    state: AskTopping
        a: Какую начинку вы предпочитаете? Пепперони, Маргарита, Гавайская, мясная или овощная. Можно сразу указать другие параметры.

    state: AskSize
        a: Какой размер пиццы? Маленькая 30 см, средняя 35 см или большая 40 см. Можно сразу указать другие параметры.

    state: AskDough
        a: Какая основа пиццы? Тонкое или традиционное тесто. Можно сразу указать другие параметры.

    state: AskAddress
        a: Назовите адрес с улицей и номером дома, например улица Ленина дом 5, или выберите самовывоз.

    state: ConfirmOrder
        script:
            $reactions.answer(pizzaSummary($session));
        a: Всё верно? Подтвердите заказ или назовите, что изменить.
        buttons:
            "Да, подтверждаю" -> /ProcessingOrder
            "Изменить заказ" -> /ChangeOrder
            "Сбросить всё" -> /ResetOrder

    state: ConfirmInput
        intent!: /ConfirmIntent
        go!: /ProcessingOrder

    state: ProcessingOrder
        script:
            var confirmation = pizzaConfirm($session);
            if (confirmation.accepted) {
                $reactions.answer('Спасибо! Учебный заказ №' + confirmation.number + ' подтверждён. ' + pizzaSummary($session));
                $reactions.answer('Это учебный бот: передача заказа в пиццерию не подключена.');
                $reactions.transition('/AfterOrder');
            } else if (confirmation.reason === 'duplicate') {
                $reactions.answer('Этот заказ уже подтверждён. Для новой пиццы скажите «новый заказ».');
                $reactions.transition('/AfterOrder');
            } else {
                $reactions.answer('Сначала укажите недостающие параметры заказа.');
                $reactions.transition(confirmation.next);
            }

    state: AfterOrder
        a: Вы можете начать новый заказ или изменить параметры текущего.

    state: ChangeInput
        intent!: /ChangeIntent
        script:
            var editResult = pizzaApply($session, $parseTree, $entities, $request.query);
            if (editResult.errors.length) $reactions.answer(editResult.errors.join(' '));
            if (editResult.changed.length) {
                $reactions.answer('Параметры заказа сохранены.');
                $reactions.transition('/EvaluateOrder');
            }
            else $reactions.transition('/ChangeOrder');

    state: ChangeOrder
        script:
            pizzaEnsure($session);
            $reactions.answer(pizzaSummary($session));
        a: Назовите новое значение начинки, размера, основы или адреса. Остальные параметры сохранятся.
        buttons:
            "Начинку" -> /AskTopping
            "Размер" -> /AskSize
            "Основу" -> /AskDough
            "Адрес" -> /AskAddress

    state: ResetInput
        intent!: /ResetIntent
        go!: /ResetOrder

    state: ResetOrder
        script:
            pizzaReset($session);
        a: Данные заказа сброшены. Начнём новый заказ.
        go!: /EvaluateOrder

    state: CatchAll
        event!: noMatch
        script:
            var fallbackResult = pizzaApply($session, $parseTree, $entities, $request.query);
            if (fallbackResult.errors.length) $reactions.answer(fallbackResult.errors.join(' '));
            if (!fallbackResult.changed.length) $reactions.answer('Я не понял запрос. Укажите начинку, размер, основу, адрес или самовывоз. Для сброса скажите «новый заказ».');
        go!: /EvaluateOrder
