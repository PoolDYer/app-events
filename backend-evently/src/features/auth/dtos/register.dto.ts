import { ApiProperty } from '@nestjs/swagger';
import { IsEmail, IsNotEmpty, IsString, MaxLength, MinLength } from 'class-validator';

export class RegisterDto {
    @ApiProperty() @IsString() @IsNotEmpty() @MaxLength(150)
    name: string;

    @ApiProperty() @IsEmail() @MaxLength(180)
    email: string;

    @ApiProperty() @IsString() @MinLength(8) @MaxLength(128)
    password: string;
}
