import { MigrationInterface, QueryRunner, TableColumn } from "typeorm";

export class EventCapacity1795392000000 implements MigrationInterface {
    async up(runner: QueryRunner): Promise<void> {
        await runner.addColumn("events", new TableColumn({
            name: "capacity",
            type: "int",
            unsigned: true,
            default: 100,
        }));
    }

    async down(runner: QueryRunner): Promise<void> {
        await runner.dropColumn("events", "capacity");
    }
}
