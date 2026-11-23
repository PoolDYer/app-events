import {
    ConflictException,
    Injectable,
    NotFoundException,
    Inject,
    forwardRef,
} from "@nestjs/common";
import { InjectRepository } from "@nestjs/typeorm";
import { Repository } from "typeorm";
import { EventSubscription } from "./entities/event-subscription.entity";
import { Event, EventStatus } from "../events/entities/event.entity";
import { EventsService } from "../events/events.service";
import { NotificationsGateway } from "../notifications/notifications.gateway";

@Injectable()
export class SubscriptionsService {
    constructor(
        @InjectRepository(EventSubscription)
        private readonly repo: Repository<EventSubscription>,
        @Inject(forwardRef(() => EventsService))
        private readonly events: EventsService,
        @Inject(forwardRef(() => NotificationsGateway))
        private readonly notifications: NotificationsGateway,
    ) {}

    async subscribe(userId: string, eventId: string) {
        const id = await this.repo.manager.transaction(async manager => {
            const event = await manager.getRepository(Event).findOne({
                where: { id: eventId },
                lock: { mode: "pessimistic_write" },
                loadEagerRelations: false,
            });
            if (!event) throw new NotFoundException("Event not found");
            const subscriptions = manager.getRepository(EventSubscription);
            const existing = await subscriptions.findOne({
                where: { user: { id: userId }, event: { id: eventId } },
                loadEagerRelations: false,
            });
            if (existing) return existing.id;
            if (event.status !== EventStatus.ACTIVE || event.date < new Date()) {
                throw new ConflictException("EVENT_CLOSED");
            }
            const count = await subscriptions.countBy({ event: { id: eventId } });
            if (count >= event.capacity) throw new ConflictException("EVENT_FULL");

            const subscription = subscriptions.create({ user: { id: userId }, event });
            return (await subscriptions.save(subscription)).id;
        });
        this.notifications.joinSubscribedUser(userId, eventId);
        return { id, event: await this.events.detail(eventId) };
    }

    async listByUser(userId: string) {
        const subscriptions = await this.repo.find({
            where: { user: { id: userId } },
            order: { subscribedAt: "DESC" },
        });
        const events = await this.events.withAvailability(subscriptions.map(subscription => subscription.event));
        const byId = new Map(events.map(event => [event.id, event]));
        return subscriptions.map(subscription => ({
            ...subscription,
            event: byId.get(subscription.event.id)!,
        }));
    }
}
