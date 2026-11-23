import { ApiProperty } from '@nestjs/swagger';
import { IsDateString, IsInt, IsNotEmpty, IsOptional, IsString, Max, MaxLength, Min } from 'class-validator';

export class CreateEventDto {
    @ApiProperty() @IsString() @IsNotEmpty() @MaxLength(200)
    title: string;

    @ApiProperty({ required: false }) @IsOptional() @IsString()
    description?: string;

    @ApiProperty() @IsString() @IsNotEmpty() @MaxLength(200)
    location: string;

    @ApiProperty({ description: 'ISO date-time' }) @IsDateString()
    date: string;

    @ApiProperty({ required: false, default: 100 }) @IsOptional() @IsInt() @Min(1) @Max(1000000)
    capacity?: number;
}
