import { Injectable, ForbiddenException, NotFoundException, Inject, forwardRef } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, MoreThanOrEqual } from 'typeorm';
import { Event, EventStatus } from './entities/event.entity';
import { CreateEventDto } from './dtos/create-event.dto';
import { UpdateEventDto } from './dtos/update-event.dto';
import { User } from '../users/entities/user.entity';
import { NotificationsGateway } from '../notifications/notifications.gateway';
import { EventSubscription } from '../subscriptions/entities/event-subscription.entity';

@Injectable()
export class EventsService {
    constructor(
        @InjectRepository(Event) private readonly repo: Repository<Event>,
        @Inject(forwardRef(() => NotificationsGateway))
        private readonly ws: NotificationsGateway,
    ) {}

    async listPublic() {
        // Solo eventos activos a futuro; orden por fecha asc
        const events = await this.repo.find({
            where: { status: EventStatus.ACTIVE, date: MoreThanOrEqual(new Date()) },
            order: { date: 'ASC' },
            take: 100,
        });
        return this.withAvailability(events);
    }

    async detail(id: string) {
        const event = await this.findById(id);
        if (!event) throw new NotFoundException('Event not found');
        return (await this.withAvailability([event]))[0];
    }

    async withAvailability(events: Event[]) {
        if (events.length === 0) return [];
        const counts = await this.repo.manager.getRepository(EventSubscription)
            .createQueryBuilder('subscription')
            .select('subscription.eventId', 'eventId')
            .addSelect('COUNT(*)', 'count')
            .where('subscription.eventId IN (:...ids)', { ids: events.map(event => event.id) })
            .groupBy('subscription.eventId')
            .getRawMany<{ eventId: string; count: string }>();
        const totals = new Map(counts.map(row => [row.eventId, Number(row.count)]));
        return events.map(event => ({
            ...event,
            availableSpots: event.status === EventStatus.ACTIVE && event.date >= new Date()
                ? Math.max(0, event.capacity - (totals.get(event.id) ?? 0))
                : 0,
        }));
    }

    findById(id: string) {
        return this.repo.findOne({ where: { id } });
    }

    async create(dto: CreateEventDto, creator: User) {
        const entity = this.repo.create({
            title: dto.title,
            description: dto.description,
            location: dto.location,
            date: new Date(dto.date),
            capacity: dto.capacity ?? 100,
            status: EventStatus.ACTIVE,
            createdBy: creator,
        });
        return this.repo.save(entity);
    }

    private ensurePerm(event: Event, user: { sub: string; role: string }) {
        // Admin puede todo; Organizer solo si es dueño del evento
        if (user.role === 'admin') return;
        if (user.role === 'organizer' && event.createdBy?.id === user.sub) return;
        throw new ForbiddenException('Not allowed to modify this event');
    }

    async updateWithPermissions(id: string, dto: UpdateEventDto, user: any) {
        const event = await this.findById(id);
        if (!event) throw new NotFoundException('Event not found');
        this.ensurePerm(event, user);
        Object.assign(event, {
            title: dto.title ?? event.title,
            description: dto.description ?? event.description,
            location: dto.location ?? event.location,
            date: dto.date ? new Date(dto.date) : event.date,
            capacity: dto.capacity ?? event.capacity,
        });
        const saved = await this.repo.save(event);
        this.ws.emitEventUpdated(id);
        return saved;
    }

    async removeWithPermissions(id: string, user: any) {
        const event = await this.findById(id);
        if (!event) throw new NotFoundException('Event not found');
        this.ensurePerm(event, user);
        await this.repo.remove(event);
        this.ws.emitEventCancelled(id);
        return { deleted: true };
    }

    async cancelWithPermissions(id: string, user: any) {
        const event = await this.findById(id);
        if (!event) throw new NotFoundException('Event not found');
        this.ensurePerm(event, user);
        event.status = EventStatus.CANCELLED;
        const saved = await this.repo.save(event);
        this.ws.emitEventCancelled(id);
        return saved;
    }
}
