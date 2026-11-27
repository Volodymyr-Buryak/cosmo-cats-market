package space.cosmocats.marketplace.order.application.command;

import java.util.List;
import java.util.UUID;

public record CreateOrderCommand(
        UUID buyerId,
        List<Item> items
) {
    public CreateOrderCommand {
        items = List.copyOf(items);
    }

    public record Item(
            UUID productId,
            int quantity
    ) {}
}
