package output

import (
	"encoding/json"
	"fmt"
	"io"
)

type Formatter struct {
	Format string
	Out    io.Writer
}

func (f Formatter) Write(v any) error {
	if f.Format == "json" {
		b, err := json.MarshalIndent(v, "", "  ")
		if err != nil {
			return err
		}
		_, err = fmt.Fprintln(f.Out, string(b))
		return err
	}
	_, err := fmt.Fprintln(f.Out, v)
	return err
}
