/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.test.http

import ratpack.test.http.internal.DefaultMultipartForm
import spock.lang.Specification
import spock.lang.Unroll

/*
    As defined in RFC 1867 and 2388 (see https://tools.ietf.org/html/rfc1867)
 */

class DefaultMultipartFormSpec extends Specification {

    def 'form with single field'() {
        given:
            builder.field(field.key, field.value)

        when:
            def form = builder.build()

        then:
            form.headers == ['Content-Type': "multipart/form-data; boundary=${boundary}"]
            form.body ==
"""--${boundary}
Content-Disposition: form-data; name="${field.key}"

${field.value}
--${boundary}--
"""

        where:
            builder = DefaultMultipartForm.builder()
            boundary = builder.boundary
            field = ['name': 'Werner Heisenberg'].find { true }
    }

    def 'form with multiple fields'() {
        given:
            builder.fields(fields)

        when:
            def form = builder.build()

        then:
            form.headers == ['Content-Type': "multipart/form-data; boundary=${boundary}"]
            form.body ==
"""--${boundary}
Content-Disposition: form-data; name="${name.key}"

${name.value}
--${boundary}
Content-Disposition: form-data; name="${role.key}"

${role.value}
--${boundary}--
"""

        where:
            builder = DefaultMultipartForm.builder()
            boundary = builder.boundary
            fields = ['name': 'Werner Heisenberg', 'role': 'theoretical physicist']
            name = fields.find { it.key == 'name' }
            role = fields.find { it.key == 'role' }
    }

    def 'upload single file'() {
        given:
            builder.file()
                .field(field)
                .contentType(contentType)
                .name(name)
                .data(data).add()

        when:
            def form = builder.build()

        then:
            form.headers == ['Content-Type': "multipart/form-data; boundary=${boundary}"]
            form.body ==
"""--${boundary}
Content-Disposition: form-data; name="${field}"; filename="${name}"
Content-Type: ${contentType}

${data}
--${boundary}--
"""

        where:
            builder = DefaultMultipartForm.builder()
            boundary = builder.boundary
            field = 'upload'
            name = 'filename.txt'
            contentType = 'text/plain'
            data = '<content>'
    }

    def 'upload file with specific encoding'() {
        given:
            builder.file()
                .field(field)
                .contentType(contentType)
                .encoding(encoding)
                .name(name)
                .data(data).add()

        when:
            def form = builder.build()

        then:
            form.headers == ['Content-Type': "multipart/form-data; boundary=${boundary}"]
            form.body ==
"""--${boundary}
Content-Disposition: form-data; name="${field}"; filename="${name}"
Content-Type: ${contentType}
Content-Transfer-Encoding: ${encoding}

${data}
--${boundary}--
"""

        where:
            builder = DefaultMultipartForm.builder()
            boundary = builder.boundary
            encoding = 'binary'
            field = 'upload'
            name = 'filename.jpg'
            contentType = 'image/jpg'
            data = '<content>'
    }

    def 'upload multiple files against single field'() {
        given:
            builder.file()
                .field(field)
                .contentType(file1.contentType)
                .name(file1.name)
                .data(file1.data).add()

        and:
            builder.file()
                .field(field)
                .contentType(file2.contentType)
                .encoding(file2.encoding)
                .name(file2.name)
                .data(file2.data).add()

        when:
            def form = builder.build()

        then:
            form.headers == ['Content-Type': "multipart/form-data; boundary=${boundary}"]
            form.body ==
"""--${boundary}
Content-Disposition: form-data; name="${field}"
Content-Type: multipart/mixed, boundary=${subBoundary}

--${subBoundary}
Content-Disposition: attachment; filename="${file1.name}"
Content-Type: ${file1.contentType}

${file1.data}
--${subBoundary}
Content-Disposition: attachment; filename="${file2.name}"
Content-Type: ${file2.contentType}
Content-Transfer-Encoding: ${file2.encoding}

${file2.data}
--${subBoundary}--
--${boundary}--
"""

        where:
            builder = DefaultMultipartForm.builder()
            boundary = builder.boundary
            field = 'multiupload'
            subBoundary = "${boundary}_${field}"

            file1 = [name: 'filename.txt', contentType: 'text/plain', data: '<content>']
            file2 = [name: 'other.png', contentType: 'image/png', encoding: 'base64', data: 'UE5HSU1BR0U=']
    }

    @Unroll('#encoding file transfer encoding satisfies spec')
    def 'file transfer encoding satisfies spec'() {
        given:
            builder.file()
                .field('upload')
                .contentType('image/jpg')
                .encoding(encoding)
                .name('filename.jpg')
                .data('<content>').add()

        when:
            builder.build()

        then:
            noExceptionThrown()

        where:
            builder = DefaultMultipartForm.builder()
            encoding << ['BASE64', 'QUOTED-PRINTABLE', '8BIT', '7BIT', 'BINARY', 'X-CUSTOM-ENCODING'].collect {
                [it, it.toLowerCase()]
            }.flatten()
    }

    def 'invalid file transfer encoding is not accepted'() {
        when:
            builder.file()
                .field('upload')
                .contentType('image/jpg')
                .encoding(encoding)
                .name('filename.jpg')
                .data('<content>').add()

        then:
            thrown(IllegalArgumentException)

        where:
            builder = DefaultMultipartForm.builder()
            encoding = 'LINEAR-B'
    }

}
