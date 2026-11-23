import * as path from "path";
import { promises as fs } from "fs";
import * as argon2 from "argon2";
import { AppDataSource } from "../typeorm.config";
import { User } from "../../../features/users/entities/user.entity";
import { Event, EventStatus } from "../../../features/events/entities/event.entity";
import { Role } from "../../http/enums/role.enum";

async function resolveUsersFile(): Promise<string> {
    const candidates = [
        path.resolve(__dirname, "data/users.json"),
        path.resolve(__dirname, "./data/users.json"),
        path.resolve(process.cwd(), "src/core/database/seed/data/users.json"),
        path.resolve(process.cwd(), "dist/core/database/seed/data/users.json"),
    ];

    for (const p of candidates) {
        try {
            await fs.access(p);
            return p;
        } catch {}
    }

    throw new Error(
        "Users seed file not found. Set SEED_PARAMETERS_FILE or place users.json in a known path.",
    );
}

export async function seedUser() {
    await AppDataSource.initialize();
    const runner = AppDataSource.createQueryRunner();
    await runner.connect();
    await runner.startTransaction();

    try {
        const filePath = await resolveUsersFile();
        const raw = await fs.readFile(filePath, "utf-8");
        const parameters: Array<{
            name: string;
            email: string;
            password: string;
            role: string;
        }> = JSON.parse(raw);

        const repo = runner.manager.getRepository(User);
        for (const item of parameters) {
            const exists = await repo.findOne({
                where: { email: item.email },
            });
            if (exists) {
                console.log(`PhoneNumber ya existía: ${exists.email}`);
                continue;
            }

            const passwordHash = await argon2.hash(item.password);
            await repo.insert({
                name: item.name,
                email: item.email,
                passwordHash: passwordHash,
                role: item.role as Role,
                createdAt: new Date(),
                updatedAt: new Date(),
            });

            console.log(`email registrado: ${item.email}`);
        }
        if (process.env.NODE_ENV === "development") {
            const organizer = await repo.findOneBy({ role: Role.ORGANIZER });
            if (organizer) {
                const events = runner.manager.getRepository(Event);
                const start = Date.now();
                const day = 24 * 60 * 60 * 1000;
                const samples = [
                    {
                        id: "755a3923-b2ef-4a06-9a05-a33034854d72",
                        title: "Encuentro de desarrollo Android",
                        description: "Taller práctico de Kotlin y Jetpack Compose con ejemplos de aplicaciones nativas.",
                        location: "Centro Cultural de la UNSCH, Ayacucho",
                        date: new Date(start + 7 * day),
                        capacity: 40,
                    },
                    {
                        id: "c3830dde-3ff4-4f01-89fa-e4f564cfbb64",
                        title: "Feria de proyectos móviles",
                        description: "Demostración de proyectos estudiantiles y conversación con los equipos.",
                        location: "Auditorio universitario, Ayacucho",
                        date: new Date(start + 14 * day),
                        capacity: 60,
                    },
                ];
                await events.createQueryBuilder().insert().values(samples.map(event => ({
                    ...event,
                    status: EventStatus.ACTIVE,
                    createdBy: organizer,
                }))).orIgnore().execute();
            }
        }
        await runner.commitTransaction();
        console.log("Seed de users completado - OK");
    } catch (err) {
        await runner.rollbackTransaction();
        console.error("Error en seed de users - FAILED", err);
        process.exitCode = 1;
    } finally {
        await runner.release();
        await AppDataSource.destroy();
    }
}
